package com.stockpro.supplierservice.dto;

import lombok.Data;

@Data
public class SupplierRequest {

    private String name;
    private String contactName;
    private String email;
    private String phone;
    private String address;
}