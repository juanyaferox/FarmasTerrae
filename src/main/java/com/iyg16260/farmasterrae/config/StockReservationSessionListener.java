package com.iyg16260.farmasterrae.config;

import com.iyg16260.farmasterrae.service.StockReservationService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

@Component
public class StockReservationSessionListener implements HttpSessionListener {

    private final StockReservationService stockReservationService;

    public StockReservationSessionListener(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        if (session == null) {
            return;
        }
        String reservationId = stockReservationService.getReservationId(session);
        if (reservationId != null) {
            stockReservationService.releaseReservation(reservationId);
        }
    }
}
