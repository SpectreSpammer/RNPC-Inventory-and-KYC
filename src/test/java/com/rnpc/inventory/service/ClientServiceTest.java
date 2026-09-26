package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cross-format matching, duplicate detection and the stored format in ClientService. The repository
 * is mocked - no Spring context, no database.
 */
class ClientServiceTest {

    private final ClientRepository repo = mock(ClientRepository.class);
    private final ClientService service = new ClientService(repo, mock(RepairRecordService.class));

    private static Client client(long id, String name, String number) {
        Client c = new Client();
        c.setClientId(id);
        c.setFullName(name);
        c.setContactNumber(number);
        return c;
    }

    private void stored(Client... clients) {
        when(repo.findAll(any(Sort.class))).thenReturn(List.of(clients));
        when(repo.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aTicketWithAnyFormatOfTheNumberFindsTheSameClient() {
        stored(client(1, "Nand", "0917 234 5678"), client(2, "Robin", "0908 123 9876"));

        for (String typed : List.of("09172345678", "0917 234 5678", "0917-234-5678", "+639172345678",
                "+63 917 234 5678", "9172345678")) {
            assertEquals(1L, service.findByContactNumber(typed).orElseThrow().getClientId(), typed);
        }
        assertEquals(2L, service.findByContactNumber("+63 908 123 9876").orElseThrow().getClientId());
    }

    @Test
    void aStoredRowInAnOldFormatStillMatches() {
        // Rows from before the format rule hold whatever was typed.
        stored(client(1, "Nand", "+639172345678"), client(2, "Robin", "0908-123-9876"));

        assertEquals(1L, service.findByContactNumber("0917 234 5678").orElseThrow().getClientId());
        assertEquals(2L, service.findByContactNumber("09081239876").orElseThrow().getClientId());
    }

    @Test
    void aLandlineMatchesOnDigitsAlone() {
        stored(client(1, "Shop", "(02) 8123 4567"));

        assertTrue(service.findByContactNumber("02-8123-4567").isPresent());
        assertTrue(service.findByContactNumber("+63 2 8123 4567").isPresent());
    }

    @Test
    void anUnknownOrBlankNumberFindsNothing() {
        stored(client(1, "Nand", "0917 234 5678"));

        assertTrue(service.findByContactNumber("0999 000 0000").isEmpty());
        assertTrue(service.findByContactNumber("").isEmpty());
        assertTrue(service.findByContactNumber(null).isEmpty());
    }

    @Test
    void existingDuplicatesResolveToTheOldestClientInsteadOfThrowing() {
        // Two rows that share a number once normalised: the outage this batch fixes.
        stored(client(3, "Nand (old)", "0917 234 5678"), client(9, "Nand (new)", "09172345678"));

        assertEquals(3L, service.findByContactNumber("+639172345678").orElseThrow().getClientId());
        assertEquals(2, service.findAllByContactNumber("0917-234-5678").size());
    }

    @Test
    void duplicateDetectionNamesTheOtherClientAndIgnoresTheClientBeingEdited() {
        stored(client(1, "Nand Test", "0917 234 5678"), client(2, "Robin", "0908 123 9876"));

        // Adding a client (no id to exclude) with Nand's number in another format.
        Client clash = service.findOtherWithContactNumber("+63 917-234-5678", null).orElseThrow();
        assertEquals("Nand Test", clash.getFullName());
        assertEquals(1L, clash.getClientId());

        // Editing Robin to Nand's number is a clash; editing Nand and keeping his own number is not.
        assertEquals(1L, service.findOtherWithContactNumber("09172345678", 2L).orElseThrow().getClientId());
        assertTrue(service.findOtherWithContactNumber("09172345678", 1L).isEmpty());
        // A free number never clashes.
        assertTrue(service.findOtherWithContactNumber("0999 000 0000", null).isEmpty());
    }

    private static ClientDto dto(String number) {
        ClientDto dto = new ClientDto();
        dto.setFullName("New Client");
        dto.setContactNumber(number);
        dto.setEmail("new@example.com");
        dto.setAddress("Somewhere");
        return dto;
    }

    @Test
    void saveStoresTheFormattedNumber() {
        stored();

        assertEquals("0917 234 5678", service.saveClient(dto("+63 917-234-5678")).getContactNumber());
        assertEquals("0917 234 5678", service.saveClient(dto("09172345678")).getContactNumber());
        assertEquals("(02) 8123 4567", service.saveClient(dto("(02)  8123 4567")).getContactNumber());
    }

    @Test
    void updateStoresTheFormattedNumber() {
        Client existing = client(5, "Nand", "09172345678");
        stored(existing);
        when(repo.findById(5L)).thenReturn(Optional.of(existing));

        assertEquals("0908 123 9876", service.updateClient(5L, dto("0908-123-9876")).getContactNumber());
    }
}
