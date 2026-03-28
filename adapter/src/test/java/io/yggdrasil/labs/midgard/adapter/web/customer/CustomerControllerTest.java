package io.yggdrasil.labs.midgard.adapter.web.customer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.alibaba.cola.dto.MultiResponse;
import com.alibaba.cola.dto.Response;

import io.yggdrasil.labs.midgard.adapter.web.customer.convertor.CustomerWebConvertor;
import io.yggdrasil.labs.midgard.adapter.web.customer.dto.CreateCustomerRequest;
import io.yggdrasil.labs.midgard.app.customer.application.CustomerAppService;
import io.yggdrasil.labs.midgard.app.customer.dto.cmd.CreateCustomerCmd;
import io.yggdrasil.labs.midgard.app.customer.dto.co.CustomerCO;
import io.yggdrasil.labs.midgard.app.customer.dto.qry.ListCustomerQry;

class CustomerControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CustomerAppService customerAppService =
                new CustomerAppService() {
                    @Override
                    public Response createCustomer(CreateCustomerCmd cmd) {
                        return Response.buildSuccess();
                    }

                    @Override
                    public MultiResponse<CustomerCO> listCustomerByName(ListCustomerQry query) {
                        return MultiResponse.of(Collections.<CustomerCO>emptyList());
                    }
                };
        CustomerWebConvertor customerWebConvertor =
                new CustomerWebConvertor() {
                    @Override
                    public CreateCustomerCmd toCreateCustomerCmd(CreateCustomerRequest body) {
                        return new CreateCustomerCmd();
                    }
                };
        CustomerController controller =
                new CustomerController(customerAppService, customerWebConvertor);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void should_list_customers_on_rest_style_path() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isOk());
    }

    @Test
    void should_create_customer_on_rest_style_path() throws Exception {
        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "customerId": "C001",
                                          "memberId": "M001",
                                          "customerName": "Alice",
                                          "customerType": "PERSONAL",
                                          "companyName": "Alice Co",
                                          "source": "WEB"
                                        }
                                        """))
                .andExpect(status().isOk());
    }

    @Test
    void should_not_map_legacy_list_path() throws Exception {
        mockMvc.perform(get("/customer/list")).andExpect(status().isNotFound());
    }

    @Test
    void should_not_map_legacy_create_path() throws Exception {
        mockMvc.perform(post("/customer/add").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
}
