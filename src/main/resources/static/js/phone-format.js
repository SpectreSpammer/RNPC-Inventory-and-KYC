/*
 * Formats a contact-number field as it is typed. Attach with data-phone-format on the input; nothing
 * else on the page needs to know about it, so no template hardcodes a field id.
 *
 * This MIRRORS util/PhoneNumbers.java (matchKey / format) - it does not replace it. The server still
 * formats and validates on save, so if the two ever disagree the server wins. If you change the rule
 * there, change matchKey() here to match.
 *
 *   - An eleven-digit mobile is grouped 4-3-4 with single spaces: 0917 123 4567. Digits typed after
 *     the eleventh are ignored.
 *   - A leading +63 (or a bare 12-digit 639...) reads as 0, and a bare 10-digit 9... gains its 0, so
 *     pasting +639171234567 shows 0917 123 4567.
 *   - Anything that is not a digit is stripped as it arrives once the number is a mobile, so
 *     0917-123-4567 and (0917) 123-4567 also land as 0917 123 4567.
 *   - Anything else - a landline, a foreign number, or a number still too short to tell - is left as
 *     typed apart from stray characters, exactly like PhoneNumbers.format.
 *   - It never blocks a key and never clears the field: whatever it cannot recognise stays as typed.
 *
 * The caret is preserved by counting the digits before it, reformatting, and putting it back after
 * the same number of digits, so editing in the middle does not jump to the end.
 */
(function () {
    'use strict';

    var MOBILE_DIGITS = 11;

    /**
     * Mirror of PhoneNumbers.matchKey, plus how the leading digits changed so the caret can follow:
     * {key, consumed: raw digits replaced at the front, added: digits put there instead}.
     */
    function analyse(raw, isDeletion) {
        var digits = raw.replace(/\D/g, '');
        if (!digits) {
            return { key: '', consumed: 0, added: 0 };
        }
        var plus63 = raw.replace(/\s/g, '').indexOf('+63') === 0;
        var bareMobile63 = digits.length === 12 && digits.indexOf('639') === 0;
        if (digits.indexOf('63') === 0 && (plus63 || bareMobile63)) {
            var rest = digits.slice(2);
            var consumed = 2;
            if (rest.charAt(0) === '0') {
                rest = rest.slice(1);
                consumed = 3;
            }
            return { key: '0' + rest, consumed: consumed, added: 1 };
        }
        // Not while deleting: otherwise removing the leading 0 of a complete number would put it
        // straight back and that digit could never be edited.
        if (!isDeletion && digits.length === 10 && digits.charAt(0) === '9') {
            return { key: '0' + digits, consumed: 0, added: 1 };
        }
        return { key: digits, consumed: 0, added: 0 };
    }

    /** Stray characters out, runs of whitespace collapsed, leading whitespace dropped (trailing kept while typing). */
    function cleanOther(raw) {
        return raw.replace(/[^0-9+()\-\s]/g, '').replace(/\s+/g, ' ').replace(/^ /, '');
    }

    function group(key) {
        return [key.slice(0, 4), key.slice(4, 7), key.slice(7, 11)].filter(Boolean).join(' ');
    }

    /** Only a number that starts 09 is treated as a mobile; "0" alone or "02..." stays as typed. */
    function isMobileKey(key) {
        return /^09/.test(key);
    }

    /**
     * @param raw   what is in the field now
     * @param caret where the caret is in raw (or null)
     * @param isDeletion true when the edit removed text, so a missing leading 0 is not restored
     * @return {text, caret, mobile}
     */
    function format(raw, caret, isDeletion) {
        var info = analyse(raw, isDeletion);
        var before = caret == null ? null : raw.slice(0, caret);

        if (!isMobileKey(info.key)) {
            var text = cleanOther(raw);
            return { text: text, caret: before == null ? null : Math.min(cleanOther(before).length, text.length), mobile: false };
        }

        var key = info.key.slice(0, MOBILE_DIGITS);
        var out = group(key);
        var newCaret = null;
        if (before != null) {
            var n = before.replace(/\D/g, '').length;
            var mapped;
            if (n === 0) {
                mapped = 0;
            } else if (n >= info.consumed) {
                mapped = n - info.consumed + info.added;
            } else {
                mapped = Math.min(n, info.added);
            }
            newCaret = positionAfterDigits(out, Math.min(mapped, key.length));
        }
        return { text: out, caret: newCaret, mobile: true };
    }

    /** Index in text just after its n-th digit (0 for n = 0, text.length once n covers every digit). */
    function positionAfterDigits(text, n) {
        if (n <= 0) {
            return 0;
        }
        var seen = 0;
        for (var i = 0; i < text.length; i++) {
            if (/\d/.test(text.charAt(i)) && ++seen === n) {
                return i + 1;
            }
        }
        return text.length;
    }

    /** Index in raw of its n-th digit (0-based), or -1. */
    function indexOfDigit(raw, n) {
        var seen = -1;
        for (var i = 0; i < raw.length; i++) {
            if (/\d/.test(raw.charAt(i)) && ++seen === n) {
                return i;
            }
        }
        return -1;
    }

    function attach(input) {
        if (input.dataset.phoneFormatBound) {
            return;
        }
        input.dataset.phoneFormatBound = 'true';

        // What the field held after the last reformat: lets a backspace or delete that only removed
        // a separator (the space between groups) be turned into removing the neighbouring digit.
        var last = { digits: '', mobile: false };

        function apply(raw, caret, isDeletion) {
            var result = format(raw, caret, isDeletion);
            if (input.value !== result.text) {
                input.value = result.text;
            }
            if (result.caret != null && document.activeElement === input && input.setSelectionRange) {
                input.setSelectionRange(result.caret, result.caret);
            }
            last = { digits: result.text.replace(/\D/g, ''), mobile: result.mobile };
        }

        input.addEventListener('input', function (event) {
            // The profile page makes this field read-only while it shows a masked value; that text
            // is not a number and must not be reformatted. Also leave IME composition alone.
            if (input.readOnly || event.isComposing) {
                return;
            }
            var raw = input.value;
            var caret = input.selectionStart;

            // Backspace / Delete on the space between groups changes no digit, so the reformat would
            // simply put the space back and the key would appear to do nothing. In a mobile number,
            // take the neighbouring digit instead.
            if (last.mobile && caret != null && raw.replace(/\D/g, '') === last.digits) {
                var n = raw.slice(0, caret).replace(/\D/g, '').length;
                var target = -1;
                if (event.inputType === 'deleteContentBackward' && n > 0) {
                    target = indexOfDigit(raw, n - 1);
                } else if (event.inputType === 'deleteContentForward') {
                    target = indexOfDigit(raw, n);
                }
                if (target >= 0) {
                    raw = raw.slice(0, target) + raw.slice(target + 1);
                    caret = target;
                }
            }
            apply(raw, caret, /^delete/.test(event.inputType || ''));
        });

        input.addEventListener('blur', function () {
            if (!input.readOnly) {
                var trimmed = format(input.value, null).text.replace(/\s+$/, '');
                if (input.value !== trimmed) {
                    input.value = trimmed;
                }
                last = { digits: trimmed.replace(/\D/g, ''), mobile: last.mobile };
            }
        });

        // A value the server rendered (an edit form, or a form redisplayed after an error) gets the
        // same treatment on load. The caret is null so nothing steals focus.
        if (!input.readOnly) {
            apply(input.value, null);
        } else {
            last = { digits: input.value.replace(/\D/g, ''), mobile: false };
        }
    }

    function attachAll(root) {
        Array.prototype.forEach.call((root || document).querySelectorAll('[data-phone-format]'), attach);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { attachAll(); });
    } else {
        attachAll();
    }

    // Exposed for a page that adds a field later, and so the rule can be exercised directly.
    window.RnpcPhoneFormat = { attach: attach, attachAll: attachAll, format: format, analyse: analyse };
})();
