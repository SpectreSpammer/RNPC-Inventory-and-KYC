package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.CellphonePartsDto;
import com.rnpc.inventory.entity.CellphoneParts;
import com.rnpc.inventory.entity.CellphoneParts.PartType;
import com.rnpc.inventory.entity.PartCondition;
import com.rnpc.inventory.repository.CellphonePartsRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The save and update in CellphonePartsService: the type is set from the caller, other types' spec
 * fields are nulled, and a stray null-type row is saved back as OTHER. The repository is mocked and
 * no photo is sent, so this needs no Spring context, no database and writes no file.
 */
class CellphonePartsServiceTest {

    private final CellphonePartsRepository repo = mock(CellphonePartsRepository.class);
    private final CellphonePartsService service = new CellphonePartsService(repo);

    {
        when(repo.save(any(CellphoneParts.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CellphonePartsDto screenDtoWithStrayFields() {
        CellphonePartsDto dto = new CellphonePartsDto();
        dto.setBrand("Samsung");
        dto.setPartName("  Galaxy A12 screen  ");
        dto.setCompatibleModels("Galaxy A12, A125F");
        dto.setPartNumber("");
        dto.setPartCondition(PartCondition.OEM_PULL);
        dto.setStocks(2);
        dto.setPrice(1800);
        dto.setDescription("   ");
        dto.setScreenPanelType("AMOLED");
        dto.setScreenGrade("Service pack");
        dto.setScreenSizeInches(6.5);
        dto.setScreenWithFrame(true);
        // Posted for other types (e.g. a tampered form) - must not be stored.
        dto.setBatteryCapacityMah(5000);
        dto.setPortConnector("USB-C");
        dto.setFlexFunction("Antenna");
        dto.setCameraMegapixels(50.0);
        return dto;
    }

    @Test
    void saveSetsTheTypeAndNullsOtherTypesSpecs() {
        CellphoneParts saved = service.saveTypedPart(PartType.SCREEN, screenDtoWithStrayFields());

        assertEquals(PartType.SCREEN, saved.getPartType());
        assertEquals(PartCondition.OEM_PULL, saved.getPartCondition());
        assertEquals("Galaxy A12 screen", saved.getPartName());
        assertEquals("AMOLED", saved.getScreenPanelType());
        assertEquals(6.5, saved.getScreenSizeInches());
        assertEquals(Boolean.TRUE, saved.getScreenWithFrame());

        assertNull(saved.getBatteryCapacityMah());
        assertNull(saved.getPortConnector());
        assertNull(saved.getFlexFunction());
        assertNull(saved.getCameraMegapixels());

        // Blank optional text is stored as null, not "".
        assertNull(saved.getPartNumber());
        assertNull(saved.getNotes());

        // No photo sent: nothing uploaded and no file name.
        assertNull(saved.getImageFileName());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void updateKeepsTheStoredTypeAndPhoto() {
        CellphoneParts existing = new CellphoneParts();
        existing.setCellphonePartId(7L);
        existing.setPartType(PartType.BATTERY);
        existing.setImageFileName("123_battery.png");
        when(repo.findById(7L)).thenReturn(Optional.of(existing));

        CellphonePartsDto dto = screenDtoWithStrayFields();
        dto.setBatteryVoltage(3.87);
        CellphoneParts saved = service.updateCellphonePart(7L, dto);

        assertEquals(PartType.BATTERY, saved.getPartType());
        assertEquals(5000, saved.getBatteryCapacityMah());
        assertEquals(3.87, saved.getBatteryVoltage());
        assertNull(saved.getScreenPanelType());
        assertNull(saved.getScreenSizeInches());
        assertNull(saved.getPortConnector());
        assertEquals("123_battery.png", saved.getImageFileName());
    }

    @Test
    void aStrayNullTypeRowIsSavedBackAsOther() {
        CellphoneParts existing = new CellphoneParts();
        existing.setCellphonePartId(3L);
        when(repo.findById(3L)).thenReturn(Optional.of(existing));

        CellphonePartsDto dto = new CellphonePartsDto();
        dto.setBrand("Apple");
        dto.setPartName("SIM tray");
        dto.setPartCondition(PartCondition.NEW);
        dto.setStocks(4);
        dto.setPrice(150);
        dto.setDescription("Nano-SIM tray, silver");
        dto.setScreenSizeInches(6.1);   // stray spec from another type: cleared
        CellphoneParts saved = service.updateCellphonePart(3L, dto);

        assertEquals(PartType.OTHER, saved.getPartType());
        assertEquals("SIM tray", saved.getPartName());
        assertEquals("Nano-SIM tray, silver", saved.getNotes());
        assertNull(saved.getScreenSizeInches());
    }

    @Test
    void toDtoCopiesTheTypedFieldsBackForTheEditForm() {
        CellphoneParts part = service.saveTypedPart(PartType.SCREEN, screenDtoWithStrayFields());
        CellphonePartsDto dto = service.toDto(part);

        assertEquals("Galaxy A12 screen", dto.getPartName());
        assertEquals("Galaxy A12, A125F", dto.getCompatibleModels());
        assertEquals(PartCondition.OEM_PULL, dto.getPartCondition());
        assertEquals("Service pack", dto.getScreenGrade());
        assertNull(dto.getBatteryCapacityMah());
    }
}
