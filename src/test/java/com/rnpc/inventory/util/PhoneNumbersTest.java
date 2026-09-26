package com.rnpc.inventory.util;

import com.rnpc.inventory.dto.AppointmentDto;
import com.rnpc.inventory.dto.CheckoutDto;
import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.dto.ProfileDto;
import com.rnpc.inventory.dto.TicketDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The contact-number rule in one place: format on store, digits-only on match, one shared pattern. */
class PhoneNumbersTest {

    private static final List<String> ALL_SPELLINGS_OF_ONE_MOBILE = List.of(
            "09172345678", "0917 234 5678", "0917-234-5678", "+639172345678", "+63 917 234 5678",
            "+63 (917) 234-5678", "639172345678", "9172345678", "0917  234   5678", "  0917 234 5678 ",
            "(0917) 234 5678", "+63 0917 234 5678");

    @Test
    void everySpellingOfAMobileFormatsToFourThreeFour() {
        for (String raw : ALL_SPELLINGS_OF_ONE_MOBILE) {
            assertEquals("0917 234 5678", PhoneNumbers.format(raw), raw);
        }
    }

    @Test
    void everySpellingOfAMobileHasTheSameMatchKey() {
        for (String raw : ALL_SPELLINGS_OF_ONE_MOBILE) {
            assertEquals("09172345678", PhoneNumbers.matchKey(raw), raw);
            assertTrue(PhoneNumbers.sameNumber("0917 234 5678", raw), raw);
        }
    }

    @Test
    void differentPhonesDoNotMatch() {
        assertFalse(PhoneNumbers.sameNumber("0917 234 5678", "0917 234 5679"));
        assertFalse(PhoneNumbers.sameNumber("0917 234 5678", "0918 234 5678"));
        assertFalse(PhoneNumbers.sameNumber("0917 234 5678", null));
        assertFalse(PhoneNumbers.sameNumber("", ""));
        assertFalse(PhoneNumbers.sameNumber(null, null));
    }

    @Test
    void formatIsIdempotent() {
        for (String raw : ALL_SPELLINGS_OF_ONE_MOBILE) {
            String once = PhoneNumbers.format(raw);
            assertEquals(once, PhoneNumbers.format(once));
        }
        assertEquals("(02) 8123 4567", PhoneNumbers.format(PhoneNumbers.format("(02)   8123 4567")));
    }

    @Test
    void aLandlineKeepsWhatWasTypedCleanedOfStrayCharactersAndStillMatchesOnDigits() {
        assertEquals("(02) 8123 4567", PhoneNumbers.format("(02)  8123 4567"));
        assertEquals("(02) 8123-4567", PhoneNumbers.format(" (02) 8123-4567 "));
        // Stray characters are dropped, the rest kept.
        assertEquals("(02) 8123 4567", PhoneNumbers.format("(02) 8123 4567 ext."));
        // Same landline, different punctuation.
        assertTrue(PhoneNumbers.sameNumber("(02) 8123 4567", "02-8123-4567"));
        assertTrue(PhoneNumbers.sameNumber("(02) 8123 4567", "028 1234 567"));
        // +63 prefix is ignored for a landline as well.
        assertTrue(PhoneNumbers.sameNumber("(02) 8123 4567", "+63 2 8123 4567"));
    }

    @Test
    void aTenDigitNumberThatIsNotAMobileIsLeftAlone() {
        assertEquals("5551234567", PhoneNumbers.matchKey("5551234567"));
        assertEquals("555 123 4567", PhoneNumbers.format("555 123 4567"));
    }

    @Test
    void aNumberThatMerelyStartsWith63IsNotTreatedAsAPrefix() {
        // Only a 12-digit 639... or an explicit +63 counts as the country code.
        assertEquals("6312345", PhoneNumbers.matchKey("631-2345"));
        assertEquals("+63 2 1234", PhoneNumbers.format("+63 2 1234"));
    }

    @Test
    void nullAndBlankAreSafe() {
        assertNull(PhoneNumbers.format(null));
        assertEquals("", PhoneNumbers.format("   "));
        assertEquals("", PhoneNumbers.matchKey(null));
        assertEquals("", PhoneNumbers.matchKey("abc"));
    }

    // ---- the shared validation pattern -----------------------------------------------------------

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean contactNumberIsValid(Object dto) {
        return validator.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .noneMatch("contactNumber"::equals);
    }

    private ClientDto client(String number) {
        ClientDto dto = new ClientDto();
        dto.setFullName("Nand Test");
        dto.setContactNumber(number);
        dto.setEmail("nand@example.com");
        dto.setAddress("123 Test St");
        return dto;
    }

    @Test
    void theFormattedFormAndEveryInputSpellingPassTheSharedPattern() {
        for (String raw : ALL_SPELLINGS_OF_ONE_MOBILE) {
            assertTrue(contactNumberIsValid(client(raw)), raw);
        }
        assertTrue(contactNumberIsValid(client("0917 234 5678")));
        assertTrue(contactNumberIsValid(client("(02) 8123 4567")));
    }

    @Test
    void junkFailsThePattern() {
        for (String bad : List.of("", "abc", "12345", "-------", "0917 234 5678 ext 9", "0917/234/5678",
                "1234567890123456", "+63 917 234 5678 9999 99")) {
            assertFalse(contactNumberIsValid(client(bad)), bad);
        }
    }

    @Test
    void allFiveDtosUseTheSamePattern() {
        String withPlusAndSpaces = "+63 917 234 5678";
        String junk = "-------";

        TicketDto ticket = new TicketDto();
        ticket.setContactNumber(withPlusAndSpaces);
        AppointmentDto appointment = new AppointmentDto();
        appointment.setContactNumber(withPlusAndSpaces);
        CheckoutDto checkout = new CheckoutDto();
        checkout.setContactNumber(withPlusAndSpaces);
        ProfileDto profile = new ProfileDto();
        profile.setContactNumber(withPlusAndSpaces);
        for (Object dto : List.of(ticket, appointment, checkout, profile)) {
            assertTrue(contactNumberIsValid(dto), dto.getClass().getSimpleName());
        }

        ticket.setContactNumber(junk);
        appointment.setContactNumber(junk);
        checkout.setContactNumber(junk);
        profile.setContactNumber(junk);
        for (Object dto : List.of(ticket, appointment, checkout, profile)) {
            assertFalse(contactNumberIsValid(dto), dto.getClass().getSimpleName());
        }
    }
}
