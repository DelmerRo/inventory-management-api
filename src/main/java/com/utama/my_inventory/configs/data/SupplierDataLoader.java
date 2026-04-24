package com.utama.my_inventory.configs.data;

import com.utama.my_inventory.entities.Supplier;
import com.utama.my_inventory.repositories.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class SupplierDataLoader {

    private final SupplierRepository supplierRepository;
    private static final Random random = new Random();

    /**
     * Carga todos los proveedores (los que falten, sin duplicar)
     */
    @Transactional
    public void load() {
        List<Supplier> suppliersToLoad = new ArrayList<>();

        // Definición de todos los proveedores esperados
        Supplier[] expectedSuppliers = {
                createSupplier("Eleven Regalos", "Juan Pérez", "contacto@elevenregalos.com", "+54 9 11 6265-6294", "Sarmiento 2224, CABA"),
                createSupplier("Plasticuer", "Guadalupe", "ventas@plasticuer.com", "+54 11 6856-3545", "Pres. José Evaristo Uriburu 366, C1025 Cdad. Autónoma de Buenos Aires"),
                createSupplier("Bazar Rivadavia", "Carlos López", "info@bazarrivadavia.com", "+54 11 3333-8888", "Av. Rivadavia 2241, C1034 ACB, Cdad. Autónoma de Buenos Aires"),
                createSupplier("Artesano de Tigre", "María González", "artesano@tigre.com", "+54 11 4444-8888", "Feria Nordelta"),
                createSupplier("Distribuidora Orion", "Roberto Sánchez", "orion.distribuidora.once@gmail.com", "+54 11 8107-0900", "Pasteur 64, Cdad. Autónoma de Buenos Aires"),
                createSupplier("Supermayorista Vital", "Vital", "legal@vital.com.ar", "+54 11 4553-6700", "Tronador 400, C1427CRJ Cdad. Autónoma de Buenos Aires")
        };

        // Verificar qué proveedores faltan
        for (Supplier expected : expectedSuppliers) {
            boolean exists = supplierRepository.findByNameAndActiveTrue(expected.getName()).isPresent();
            if (!exists) {
                suppliersToLoad.add(expected);
            }
        }

        if (suppliersToLoad.isEmpty()) {
            System.out.println("✅ Todos los proveedores ya están cargados.");
            return;
        }

        supplierRepository.saveAll(suppliersToLoad);
        System.out.println("✅ Proveedores creados: " + suppliersToLoad.size());
        suppliersToLoad.forEach(s -> System.out.println("   - " + s.getName()));
    }

    /**
     * Carga solo los proveedores esenciales (los que falten, sin duplicar)
     */
    @Transactional
    public void loadEssential() {
        List<Supplier> suppliersToLoad = new ArrayList<>();

        // Definición de proveedores esenciales
        Supplier[] essentialSuppliers = {
                createSupplier("Eleven Regalos", "Juan Pérez", "contacto@elevenregalos.com", "+54 9 11 6265-6294", "Sarmiento 2224, CABA"),
                createSupplier("Plasticuer", "Guadalupe", "ventas@plasticuer.com", "+54 11 6856-3545", "Pres. José Evaristo Uriburu 366, C1025 Cdad. Autónoma de Buenos Aires"),
                createSupplier("Bazar Rivadavia", "Carlos López", "info@bazarrivadavia.com", "+54 11 3333-8888", "Av. Rivadavia 2241, C1034 ACB, Cdad. Autónoma de Buenos Aires")
        };

        // Verificar qué proveedores esenciales faltan
        for (Supplier expected : essentialSuppliers) {
            boolean exists = supplierRepository.findByNameAndActiveTrue(expected.getName()).isPresent();
            if (!exists) {
                suppliersToLoad.add(expected);
            }
        }

        if (suppliersToLoad.isEmpty()) {
            System.out.println("✅ Todos los proveedores esenciales ya están cargados.");
            return;
        }

        supplierRepository.saveAll(suppliersToLoad);
        System.out.println("✅ Proveedores esenciales creados: " + suppliersToLoad.size());
        suppliersToLoad.forEach(s -> System.out.println("   - " + s.getName()));
    }

    /**
     * Fuerza la recarga completa (borra y vuelve a crear todos los proveedores)
     * Útil para reiniciar datos en desarrollo
     */
    @Transactional
    public void forceReload() {
        System.out.println("🔄 Forzando recarga completa de proveedores...");

        // Eliminar todos los proveedores existentes
        supplierRepository.deleteAll();

        // Crear todos los proveedores nuevamente
        List<Supplier> suppliers = List.of(
                createSupplier("Eleven Regalos", "Juan Pérez", "contacto@elevenregalos.com", "+54 9 11 6265-6294", "Sarmiento 2224, CABA"),
                createSupplier("Plasticuer", "Guadalupe", "ventas@plasticuer.com", "+54 11 6856-3545", "Pres. José Evaristo Uriburu 366, C1025 Cdad. Autónoma de Buenos Aires"),
                createSupplier("Bazar Rivadavia", "Carlos López", "info@bazarrivadavia.com", "+54 11 3333-8888", "Av. Rivadavia 2241, C1034 ACB, Cdad. Autónoma de Buenos Aires"),
                createSupplier("Artesano de Tigre", "María González", "artesano@tigre.com", "+54 11 4444-8888", "Feria Nordelta"),
                createSupplier("Distribuidora Orion", "Roberto Sánchez", "orion.distribuidora.once@gmail.com", "+54 11 8107-0900", "Pasteur 64, Cdad. Autónoma de Buenos Aires")
        );

        supplierRepository.saveAll(suppliers);
        System.out.println("✅ Proveedores recargados completamente: " + suppliers.size());
        suppliers.forEach(s -> System.out.println("   - " + s.getName()));
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

    /**
     * Obtiene un proveedor aleatorio
     */
    public Supplier getRandomSupplier() {
        List<Supplier> suppliers = supplierRepository.findAll();
        if (suppliers.isEmpty()) {
            throw new RuntimeException("No hay proveedores disponibles");
        }
        return suppliers.get(random.nextInt(suppliers.size()));
    }

    /**
     * Obtiene el primer proveedor (útil para tests)
     */
    public Supplier getDefaultSupplier() {
        return supplierRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hay proveedores disponibles. Ejecute el DataLoader primero."));
    }

    /**
     * Obtiene un proveedor por nombre
     */
    public Supplier getSupplierByName(String name) {
        return supplierRepository.findByNameAndActiveTrue(name)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado: " + name));
    }

    /**
     * Obtiene todos los proveedores
     */
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    /**
     * Cuenta cuántos proveedores hay
     */
    public long count() {
        return supplierRepository.count();
    }
}