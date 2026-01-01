package com.hutech.bookstore.repository;

import com.hutech.bookstore.model.Address;
import com.hutech.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(User user);
    Optional<Address> findByUserAndIsDefaultTrueAndIsDeletedFalse(User user);
}

