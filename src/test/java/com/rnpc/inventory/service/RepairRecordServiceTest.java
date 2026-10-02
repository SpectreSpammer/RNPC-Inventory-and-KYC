package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.TicketDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.repository.ClientRepository;
import com.rnpc.inventory.repository.RepairRecordRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * createFromTicket (the walk-in ticket's own repair-creation path) and its receivedByEmployeeId
 * attribution (batch 6 - see TicketController.createTicket, the "Ticket created" page). Repository
 * mocked - no Spring context, no database.
 */
class RepairRecordServiceTest {

    private final RepairRecordRepository repo = mock(RepairRecordRepository.class);
    private final ClientRepository clientRepo = mock(ClientRepository.class);
    private final RepairRecordService service = new RepairRecordService(repo, clientRepo);

    private void stored() {
        when(repo.save(any(RepairRecord.class))).thenAnswer(inv -> {
            RepairRecord r = inv.getArgument(0);
            if (r.getRepairId() == null) {
                r.setRepairId(11L);
            }
            return r;
        });
    }

    private static TicketDto ticket() {
        TicketDto dto = new TicketDto();
        dto.setFullName("Walk-in Customer");
        dto.setContactNumber("0999 000 0000");
        dto.setDeviceType("Desktop");
        dto.setModelName("Ryzen 5 3200G");
        dto.setIssueDescription("no display");
        return dto;
    }

    @Test
    void createFromTicketRecordsTheActingAdminAsReceivedBy() {
        stored();
        Client client = new Client();
        client.setClientId(1L);

        RepairRecord repair = service.createFromTicket(client, ticket(), "Test Admin-12345");

        assertEquals("Test Admin-12345", repair.getReceivedByEmployeeId());
        assertEquals("JO-00011", repair.getJobOrderNumber());
        assertEquals(RepairRecord.RepairStatus.PENDING, repair.getStatus());
    }

    @Test
    void createFromTicketAllowsANullReceivedBy() {
        // authentication is never actually null on this route in production (the filter chain
        // requires an admin), but TicketController still guards it - see its own comment.
        stored();
        Client client = new Client();
        client.setClientId(1L);

        RepairRecord repair = service.createFromTicket(client, ticket(), null);

        assertNull(repair.getReceivedByEmployeeId());
    }
}
