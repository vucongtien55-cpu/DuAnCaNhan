package com.novamart.service.impl;

import com.novamart.dto.request.CreateAddressRequest;
import com.novamart.dto.request.UpdateAddressRequest;
import com.novamart.dto.response.AddressResponse;
import com.novamart.entity.Address;
import com.novamart.entity.User;
import com.novamart.exception.ResourceNotFoundException;
import com.novamart.repository.AddressRepository;
import com.novamart.repository.UserRepository;
import com.novamart.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<AddressResponse> getMyAddresses(String userEmail) {
        User user = getUserByEmail(userEmail);
        return addressRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse createAddress(String userEmail, CreateAddressRequest request) {
        User user = getUserByEmail(userEmail);

        // Nếu người dùng chọn đặt địa chỉ mới làm mặc định, hủy trạng thái mặc định của các địa chỉ cũ
        if (request.isDefault()) {
            clearDefaultAddress(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .detail(request.getDetail())
                .isDefault(request.isDefault())
                .build();

        return mapToResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(String userEmail, Long addressId, UpdateAddressRequest request) {
        User user = getUserByEmail(userEmail);

        // Kiểm tra quyền sở hữu địa chỉ (chỉ cho phép sửa địa chỉ thuộc về chính user đó)
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ hoặc bạn không có quyền sửa"));

        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setDetail(request.getDetail());

        return mapToResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(String userEmail, Long addressId) {
        User user = getUserByEmail(userEmail);
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ hoặc bạn không có quyền xóa"));

        addressRepository.delete(address);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(String userEmail, Long addressId) {
        User user = getUserByEmail(userEmail);
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ"));

        // Bỏ mặc định địa chỉ cũ và gán mặc định cho địa chỉ được chọn
        clearDefaultAddress(user.getId());
        address.setDefault(true);

        return mapToResponse(addressRepository.save(address));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private void clearDefaultAddress(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(existingDefault -> {
                    existingDefault.setDefault(false);
                    addressRepository.save(existingDefault);
                });
    }

    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .detail(address.getDetail())
                .isDefault(address.isDefault())
                .build();
    }
}