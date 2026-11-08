package com.novamart.service;

import com.novamart.dto.request.CreateAddressRequest;
import com.novamart.dto.request.UpdateAddressRequest;
import com.novamart.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    // Lấy danh sách địa chỉ nhận hàng của người dùng
    List<AddressResponse> getMyAddresses(String userEmail);

    // Thêm địa chỉ nhận hàng mới
    AddressResponse createAddress(String userEmail, CreateAddressRequest request);

    // Cập nhật thông tin địa chỉ nhận hàng
    AddressResponse updateAddress(String userEmail, Long addressId, UpdateAddressRequest request);

    // Xóa địa chỉ nhận hàng
    void deleteAddress(String userEmail, Long addressId);

    // Thiết lập địa chỉ mặc định
    AddressResponse setDefaultAddress(String userEmail, Long addressId);
}