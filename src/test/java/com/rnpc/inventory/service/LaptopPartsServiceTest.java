package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.LaptopPartsDto;
import com.rnpc.inventory.entity.LaptopParts;
import com.rnpc.inventory.entity.LaptopParts.PartCondition;
import com.rnpc.inventory.entity.LaptopParts.PartType;
import com.rnpc.inventory.repository.LaptopPartsRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The type-based save and update in LaptopPartsService: the type is set from the caller, other
 * types' spec fields are nulled, and a stray null-type row is saved back as OTHER. The repository is
 * mocked and no photo is sent, so this needs no Spring context, no database and writes no file.
 */
class LaptopPartsServiceTest {

    private final LaptopPartsRepository repo = mock(LaptopPartsRepository.class);
    private final LaptopPartsService service = new LaptopPartsService(repo);

    {
        when(repo.save(any(LaptopParts.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private LaptopPartsDto lcdDtoWithStrayBatteryFields() {
        LaptopPartsDto dto = new LaptopPartsDto();
        dto.setBrand("Dell");
        dto.setPartName("  Inspiron 15 3000 LCD  ");
        dto.setCompatibleModels("Inspiron 15 3511, 3520");
        dto.setPartNumber("");
        dto.setPartCondition(PartCondition.OEM_PULL);
        dto.setStocks(2);
        dto.setPrice(4800);
        dto.setDescription("   ");
        dto.setLcdSizeInches(15.6);
        dto.setLcdResolution("FHD 1920x1080");
        dto.setLcdPanelType("IPS");
        dto.setLcdConnector("30-pin eDP");
        dto.setLcdSurface("");
        dto.setLcdTouch(false);
        // Posted for another type (e.g. a tampered form) - must not be stored.
        dto.setBatteryCapacityWh(42.0);
        dto.setBatteryChemistry("Li-ion");
        dto.setRamCapacityGb(8);
        return dto;
    }

    @Test
    void typedSaveSetsTheTypeAndNullsOtherTypesSpecs() {
        LaptopParts saved = service.saveTypedPart(PartType.LCD, lcdDtoWithStrayBatteryFields());

        assertEquals(PartType.LCD, saved.getPartType());
        assertEquals(PartCondition.OEM_PULL, saved.getPartCondition());
        assertEquals("Inspiron 15 3000 LCD", saved.getPartName());
        assertEquals(15.6, saved.getLcdSizeInches());
        assertEquals("30-pin eDP", saved.getLcdConnector());
        assertEquals(Boolean.FALSE, saved.getLcdTouch());

        assertNull(saved.getBatteryCapacityWh());
        assertNull(saved.getBatteryChemistry());
        assertNull(saved.getRamCapacityGb());

        // Blank optional text is stored as null, not "".
        assertNull(saved.getPartNumber());
        assertNull(saved.getLcdSurface());
        assertNull(saved.getNotes());

        // No photo sent: nothing uploaded and no file name.
        assertNull(saved.getImageFileName());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void typedUpdateKeepsTheStoredTypeAndPhoto() {
        LaptopParts existing = new LaptopParts();
        existing.setLaptopPartId(7);
        existing.setPartType(PartType.BATTERY);
        existing.setImageFileName("123_battery.png");
        when(repo.findById(7)).thenReturn(Optional.of(existing));

        LaptopPartsDto dto = lcdDtoWithStrayBatteryFields();
        dto.setBatteryVoltage(11.4);
        LaptopParts saved = service.updateLaptopPart(7, dto);

        assertEquals(PartType.BATTERY, saved.getPartType());
        assertEquals(42.0, saved.getBatteryCapacityWh());
        assertEquals(11.4, saved.getBatteryVoltage());
        assertNull(saved.getLcdSizeInches());
        assertNull(saved.getLcdConnector());
        assertEquals("123_battery.png", saved.getImageFileName());
    }

    @Test
    void aStrayNullTypeRowIsSavedBackAsOther() {
        LaptopParts existing = new LaptopParts();
        existing.setLaptopPartId(3);
        when(repo.findById(3)).thenReturn(Optional.of(existing));

        LaptopPartsDto dto = new LaptopPartsDto();
        dto.setBrand("Acer");
        dto.setPartName("Speaker set");
        dto.setPartCondition(PartCondition.NEW);
        dto.setStocks(4);
        dto.setPrice(450);
        dto.setDescription("Left and right speakers");
        dto.setLcdSizeInches(15.6);   // stray spec from another type: cleared
        LaptopParts saved = service.updateLaptopPart(3, dto);

        assertEquals(PartType.OTHER, saved.getPartType());
        assertEquals("Speaker set", saved.getPartName());
        assertEquals("Left and right speakers", saved.getNotes());
        assertNull(saved.getLcdSizeInches());
    }

    @Test
    void toDtoCopiesTheTypedFieldsBackForTheEditForm() {
        LaptopParts part = service.saveTypedPart(PartType.LCD, lcdDtoWithStrayBatteryFields());
        LaptopPartsDto dto = service.toDto(part);

        assertEquals("Inspiron 15 3000 LCD", dto.getPartName());
        assertEquals("Inspiron 15 3511, 3520", dto.getCompatibleModels());
        assertEquals(PartCondition.OEM_PULL, dto.getPartCondition());
        assertEquals("FHD 1920x1080", dto.getLcdResolution());
        assertNull(dto.getBatteryCapacityWh());
    }
}
