package com.example.ecom.report.service;

import com.example.ecom.common.enums.AppModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceFactoryTest {

    private ReportServiceFactory factory;

    private ReportService orderReportService;
    private ReportService saleReportService;
    private ReportService userReportService;

    @BeforeEach
    void setUp() {
        orderReportService = new StubReportService(AppModule.ORDER);
        saleReportService = new StubReportService(AppModule.SALE);
        userReportService = new StubReportService(AppModule.USER);

        factory = new ReportServiceFactory(List.of(orderReportService, saleReportService, userReportService));
    }

    @Test
    @DisplayName("Should return OrderReportService for ORDER module")
    void getService_orderModule_returnsOrderReportService() {
        ReportService result = factory.getService(AppModule.ORDER);

        assertNotNull(result);
        assertEquals(AppModule.ORDER, result.getModule());
        assertSame(orderReportService, result);
    }

    @Test
    @DisplayName("Should return SaleReportService for SALE module")
    void getService_saleModule_returnsSaleReportService() {
        ReportService result = factory.getService(AppModule.SALE);

        assertNotNull(result);
        assertEquals(AppModule.SALE, result.getModule());
        assertSame(saleReportService, result);
    }

    @Test
    @DisplayName("Should return UserReportService for USER module")
    void getService_userModule_returnsUserReportService() {
        ReportService result = factory.getService(AppModule.USER);

        assertNotNull(result);
        assertEquals(AppModule.USER, result.getModule());
        assertSame(userReportService, result);
    }

    @Test
    @DisplayName("Should return null for module not registered")
    void getService_unknownModule_returnsNull() {
        // Factory with only order service
        ReportServiceFactory limitedFactory = new ReportServiceFactory(List.of(orderReportService));

        ReportService result = limitedFactory.getService(AppModule.SALE);

        assertNull(result);
    }

    @Test
    @DisplayName("Should handle empty list of services")
    void constructor_emptyServiceList_createsFactoryWithNoServices() {
        ReportServiceFactory emptyFactory = new ReportServiceFactory(List.of());

        assertNull(emptyFactory.getService(AppModule.ORDER));
        assertNull(emptyFactory.getService(AppModule.SALE));
        assertNull(emptyFactory.getService(AppModule.USER));
    }

    // Minimal stub for unit testing factory wiring
    private static class StubReportService extends ReportService {
        private final AppModule module;

        StubReportService(AppModule module) {
            super(null); // JdbcTemplate not used for factory tests
            this.module = module;
        }

        @Override
        public AppModule getModule() {
            return module;
        }

        @Override
        public List<String> getHeaders() {
            return List.of();
        }

        @Override
        public String getBaseSql() {
            return "";
        }

        @Override
        public void mapRow(java.sql.ResultSet rs, RowConsumer consumer) {
        }
    }
}
