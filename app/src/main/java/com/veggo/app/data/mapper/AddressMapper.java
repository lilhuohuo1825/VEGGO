package com.veggo.app.data.mapper;

import com.veggo.app.data.remote.dto.AddressDto;
import com.veggo.app.domain.model.Address;

import java.util.ArrayList;
import java.util.List;

public final class AddressMapper {
    private AddressMapper() {
    }

    public static Address fromDto(AddressDto dto) {
        return new Address(
                dto.getId(),
                dto.getUserId(),
                dto.getName(),
                dto.getPhone(),
                dto.getEmail(),
                dto.getProvince(),
                dto.getDistrict(),
                dto.getWard(),
                dto.getDetail(),
                dto.isDefault()
        );
    }

    public static List<Address> fromDtoList(List<AddressDto> dtos) {
        List<Address> addresses = new ArrayList<>();
        if (dtos == null) {
            return addresses;
        }
        for (AddressDto dto : dtos) {
            addresses.add(fromDto(dto));
        }
        return addresses;
    }
}
