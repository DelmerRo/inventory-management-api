package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.LoginRequestDTO;
import com.utama.my_inventory.dtos.response.LoginResponseDTO;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO requestDTO);

    void logout();

}
