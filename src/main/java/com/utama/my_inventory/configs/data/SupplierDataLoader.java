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
                createSupplier("Textiles del Valle", "María González", "textiles@valle.com", "+54 11 4321 9876", "Av. Libertador 1234, Buenos Aires"),
                createSupplier("Decoración Premium", "Carlos López", "decor@premium.com", "+54 351 456 7890", "Calle San Martín 567, Córdoba"),
                createSupplier("Muebles y Más", "Ana Rodríguez", "ventas@mueblesymas.com", "+54 341 654 3210", "Av. Pellegrini 890, Rosario"),
                createSupplier("Iluminación Moderna", "Pedro Sánchez", "info@iluminacion.com", "+54 261 789 0123", "Ruta 40 km 1200, Mendoza"),
                createSupplier("Arte y Diseño", "Laura Fernández", "arte@diseno.com", "+54 299 123 4567", "Av. Argentina 456, Neuquén"),
                createSupplier("Importaciones Elegance", "Roberto Díaz", "import@elegance.com", "+54 11 8765 4321", "Calle Florida 789, CABA"),
                createSupplier("Textures Home", "Sofía Martínez", "contacto@textures.com", "+54 381 234 5678", "Av. Sarmiento 321, Tucumán"),
                createSupplier("Natural Elements", "Diego Pérez", "info@natural.com", "+54 280 456 7891", "Ruta Nacional 3, Bahía Blanca")
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
                createSupplier("Textiles del Valle", "María González", "textiles@valle.com", "+54 11 4321 9876", "Av. Libertador 1234, Buenos Aires"),
                createSupplier("Decoración Premium", "Carlos López", "decor@premium.com", "+54 351 456 7890", "Calle San Martín 567, Córdoba"),
                createSupplier("Muebles y Más", "Ana Rodríguez", "ventas@mueblesymas.com", "+54 341 654 3210", "Av. Pellegrini 890, Rosario")
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

    // Agregar este método a SupplierDataLoader

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }
}