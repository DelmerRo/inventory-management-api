package com.utama.my_inventory.configs.data;

import com.utama.my_inventory.entities.Supplier;
import com.utama.my_inventory.repositories.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class SupplierDataLoader {

    private final SupplierRepository supplierRepository;
    private static final Random random = new Random();

    @Transactional
    public void load() {
        if (supplierRepository.count() > 0) {
            return;
        }

        List<Supplier> suppliers = Arrays.asList(
                createSupplier("Eleven Regalos", "Contacto Genérico", "contacto@elevenregalos.com", "+54 9 11 6265-6294", "Sarmiento 2224, CABA"),
                createSupplier("Plasticuer", "Guadalupe", "ventas@plasticuer.com", "+54 11 6856-3545", "Pres. José Evaristo Uriburu 366, C1025 Cdad. Autónoma de Buenos Aires"),
                createSupplier("Bazar Rivadavia", "Contacto Genérico", "info@bazarrivadavia.com", "+54 11 3333-8888", "Av. Rivadavia 2241, C1034 ACB, Cdad. Autónoma de Buenos Aires")
        );

        supplierRepository.saveAll(suppliers);
        System.out.println("✅ Proveedores creados: " + suppliers.size());
    }

    @Transactional
    public void loadEssential() {
        if (supplierRepository.count() > 0) {
            return;
        }

        List<Supplier> suppliers = Arrays.asList(
                createSupplier("Eleven Regalos", "Contacto Genérico", "contacto@elevenregalos.com", "+54 9 11 6265-6294", "Sarmiento 2224, CABA"),
                createSupplier("Plasticuer", "Guadalupe", "ventas@plasticuer.com", "+54 11 6856-3545", "Pres. José Evaristo Uriburu 366, C1025 Cdad. Autónoma de Buenos Aires"),
                createSupplier("Bazar Rivadavia", "Contacto Genérico", "info@bazarrivadavia.com", "+54 11 3333-8888", "Av. Rivadavia 2241, C1034 ACB, Cdad. Autónoma de Buenos Aires")
        );

        supplierRepository.saveAll(suppliers);
        System.out.println("✅ Proveedores esenciales creados: " + suppliers.size());
    }

    private Supplier createSupplier(String name, String contact, String email, String phone, String address) {
        return Supplier.builder()
                .name(name)
                .contactPerson(contact)
                .email(email)
                .phone(phone)
                .address(address)
                .active(true)
                .build();
    }

    public Supplier getRandomSupplier() {
        List<Supplier> suppliers = supplierRepository.findAll();
        if (suppliers.isEmpty()) {
            throw new RuntimeException("No hay proveedores disponibles");
        }
        return suppliers.get(random.nextInt(suppliers.size()));
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }
}