package com.rnpc.inventory.controller;

import com.rnpc.inventory.service.CpuPartsService;
import com.rnpc.inventory.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * removePhoto is called by fetch (fragments/parts-form :: remove-photo-script), not a browser form
 * submission, so the wire-level HTTP method is a real DELETE. Browsers preserve the original method
 * when following a 301/302 for anything other than POST, so a bare "redirect:/cpu/edit/7" (a 302)
 * would have the browser re-send the follow-up as DELETE against the GET-only edit route, 405ing
 * even though the removal itself succeeded. A 303 See Other is always followed as GET, regardless
 * of the original method - see util.Redirects. Real CpuPartsController, mocked service; needs no
 * Spring context, templates or database. Every removePhoto handler follows this same rule (see
 * ClientControllerTest's equivalent test) - CPU stands in for the other nine.
 */
class CpuPartsControllerTest {

    private final CpuPartsService service = mock(CpuPartsService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new CpuPartsController(service, mock(NotificationService.class)))
            .build();

    @Test
    void removePhotoRedirectsWithSeeOther() throws Exception {
        mvc.perform(delete("/cpu/removePhoto/7"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/cpu/edit/7"));
        verify(service).removePhoto(7);
    }
}
