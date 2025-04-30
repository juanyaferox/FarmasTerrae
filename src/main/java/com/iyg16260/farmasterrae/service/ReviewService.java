package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.mapper.ReviewMapper;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    ReviewMapper reviewMapper;

    private static final int PAGE_SIZE_USER = 10;

    public Page<ReviewDTO> getReviews(User user, int page) {
        Pageable pageable = Pageable
                .ofSize(PAGE_SIZE_USER)
                .withPage(page);

        return reviewRepository.findByUser(user, pageable)
                .map(reviewMapper::reviewToReviewDTO);
    }
}
