package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.CustomerNote;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.ShopCustomer;
import org.example.ridehailing.banhangonline.entity.ShopStaff;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.repository.CustomerNoteRepository;
import org.example.ridehailing.banhangonline.repository.ShopCustomerRepository;
import org.example.ridehailing.banhangonline.repository.ShopRepository;
import org.example.ridehailing.banhangonline.repository.ShopStaffRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.service.CustomerNoteService;
import org.example.ridehailing.banhangonline.service.ShopAuthorizationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerNoteServiceImpl implements CustomerNoteService {

    private static final int NOTE_MAX_LENGTH = 2000;

    private final CustomerNoteRepository customerNoteRepository;
    private final ShopRepository shopRepository;
    private final ShopCustomerRepository shopCustomerRepository;
    private final ShopStaffRepository shopStaffRepository;
    private final UserRepository userRepository;
    private final ShopAuthorizationService shopAuthorizationService;

    public CustomerNoteServiceImpl(
            CustomerNoteRepository customerNoteRepository,
            ShopRepository shopRepository,
            ShopCustomerRepository shopCustomerRepository,
            ShopStaffRepository shopStaffRepository,
            UserRepository userRepository,
            ShopAuthorizationService shopAuthorizationService) {
        this.customerNoteRepository = customerNoteRepository;
        this.shopRepository = shopRepository;
        this.shopCustomerRepository = shopCustomerRepository;
        this.shopStaffRepository = shopStaffRepository;
        this.userRepository = userRepository;
        this.shopAuthorizationService = shopAuthorizationService;
    }

    @Override
    public List<CustomerNote> getCustomerHistory(Long actorUserId, Long shopId, Long customerId) {
        authorizeCustomerAccess(actorUserId, shopId, customerId);
        return customerNoteRepository.findByShop_IdAndCustomer_IdOrderByCreatedAtDesc(shopId, customerId);
    }

    @Override
    public List<CustomerNote> getMyCreatedNotes(Long actorUserId, Long shopId) {
        authorizeShopMember(actorUserId, shopId);
        return customerNoteRepository.findByShop_IdAndCreatedBy_IdOrderByCreatedAtDesc(shopId, actorUserId);
    }

    @Override
    public List<CustomerNote> getMyAssignedNotes(Long actorUserId, Long shopId) {
        authorizeShopMember(actorUserId, shopId);
        return customerNoteRepository.findByShop_IdAndStaff_IdOrderByCreatedAtDesc(shopId, actorUserId);
    }

    @Override
    public List<CustomerNote> getAppointments(
            Long actorUserId, Long shopId, LocalDateTime from, LocalDateTime to) {
        authorizeShopMember(actorUserId, shopId);
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Khoảng thời gian lịch hẹn không hợp lệ.");
        }
        return customerNoteRepository.findByShop_IdAndAppointmentAtBetweenOrderByAppointmentAtAsc(
                shopId, from, to);
    }

    @Override
    @Transactional
    public CustomerNote createNote(
            Long actorUserId,
            Long shopId,
            Long customerId,
            String note,
            LocalDateTime appointmentAt) {
        Shop shop = authorizeCustomerAccess(actorUserId, shopId, customerId);
        if (note == null || note.isBlank() || note.trim().length() > NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException("Nội dung ghi chú phải có từ 1 đến 2000 ký tự.");
        }
        if (appointmentAt != null && appointmentAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Lịch hẹn không được đặt trong quá khứ.");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng."));
        User creator = userRepository.findById(actorUserId)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người thực hiện đang hoạt động."));

        CustomerNote customerNote = new CustomerNote();
        customerNote.setShop(shop);
        customerNote.setCustomer(customer);
        customerNote.setCreatedBy(creator);
        customerNote.setNote(note.trim());
        customerNote.setAppointmentAt(appointmentAt);
        shopStaffRepository.findByShop_IdAndUser_Id(shopId, actorUserId)
                .filter(staff -> Boolean.TRUE.equals(staff.getActive()))
                .ifPresent(staff -> customerNote.setStaff(staff.getUser()));
        return customerNoteRepository.save(customerNote);
    }

    // Chỉ cho phép chăm sóc khách hàng đã thuộc danh sách của đúng cửa hàng.
    private Shop authorizeCustomerAccess(Long actorUserId, Long shopId, Long customerId) {
        Shop shop = authorizeShopMember(actorUserId, shopId);
        requirePositiveId(customerId, "customerId");
        ShopCustomer membership = shopCustomerRepository.findByShop_IdAndCustomer_Id(shopId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Khách hàng chưa thuộc danh sách cửa hàng."));
        if (!"ACTIVE".equals(membership.getStatus())) {
            throw new AccessDeniedException("Khách hàng này không ở trạng thái hoạt động tại cửa hàng.");
        }
        return shop;
    }

    private Shop authorizeShopMember(Long actorUserId, Long shopId) {
        requirePositiveId(shopId, "shopId");
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cửa hàng."));
        shopAuthorizationService.requireShopMember(actorUserId, shop);
        return shop;
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
