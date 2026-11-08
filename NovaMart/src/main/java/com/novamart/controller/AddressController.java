package com.novamart.controller;

import com.novamart.dto.request.CreateAddressRequest;
import com.novamart.dto.request.UpdateAddressRequest;
import com.novamart.dto.response.AddressResponse;
import com.novamart.dto.response.ApiResponse;
import com.novamart.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    // Lấy danh sách tất cả địa chỉ nhận hàng của người dùng hiện tại
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(Authentication authentication) {
        List<AddressResponse> addresses = addressService.getMyAddresses(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách địa chỉ thành công", addresses));
    }

    // Thêm địa chỉ nhận hàng mới
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(Authentication authentication,
                                                                      @Valid @RequestBody CreateAddressRequest request) {
        AddressResponse address = addressService.createAddress(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Thêm địa chỉ thành công", address));
    }

    // Cập nhật thông tin địa chỉ nhận hàng theo ID
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(Authentication authentication,
                                                                      @PathVariable Long id,
                                                                      @Valid @RequestBody UpdateAddressRequest request) {
        AddressResponse address = addressService.updateAddress(authentication.getName(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ thành công", address));
    }

    // Xóa địa chỉ nhận hàng
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(Authentication authentication, @PathVariable Long id) {
        addressService.deleteAddress(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Xóa địa chỉ thành công", null));
    }

    // Đặt địa chỉ được chọn làm địa chỉ mặc định
    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(Authentication authentication, @PathVariable Long id) {
        AddressResponse address = addressService.setDefaultAddress(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Đặt địa chỉ mặc định thành công", address));
    }
}