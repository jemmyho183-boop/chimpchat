package com.chimpchat.api.auth.mapper;

import com.chimpchat.api.auth.dto.CurrentUserResponseDto;
import com.chimpchat.api.auth.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {
    CurrentUserResponseDto toCurrentUserResponse(UserEntity user);
}
