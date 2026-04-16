package com.chargeflow.main;

import com.chargeflow.factory.VehicleFactory;
import com.chargeflow.model.*;
import com.chargeflow.service.TripAnalyzer;
import com.chargeflow.utils.ApiConfig;
import com.chargeflow.utils.DisplayUtils;
import java.util.Scanner;

public class ChargeFlowApp {

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");

        Scanner scanner = new Scanner(System.in);
        TripAnalyzer analyzer = new TripAnalyzer();

        DisplayUtils.printBanner();

        if (ApiConfig.isConfigured()) {
            System.out.println("  [OK] API keys configured - unlimited routes enabled!");
        } else {
            System.out.println("  [WARN] API keys not configured - using offline route database.");
            System.out.println("     To enable unlimited routes, add your keys in ApiConfig.java");
            System.out.println("     -> OpenRouteService: https://openrouteservice.org/dev");
            System.out.println("     -> OpenChargeMap:    https://openchargemap.org/site/develop");
        }
        System.out.println();

        EVVehicle ev;
        ICEVehicle ice;

        System.out.println("  [SETUP] Choose comparison mode:");
        System.out.println("    1. Default vehicles (Tata Nexon EV vs Maruti Suzuki Brezza)");
        System.out.println("    2. Enter custom EV and ICE specifications");

        int mode = readChoice(scanner, "  [INPUT] Enter choice (1/2): ", 1, 2);
        if (mode == 2) {
            ev = readCustomEV(scanner);
            ice = readCustomICE(scanner);
            System.out.println();
            System.out.println("  [OK] Custom vehicles configured.");
        } else {
            ev = (EVVehicle) VehicleFactory.createVehicle("EV");
            ice = (ICEVehicle) VehicleFactory.createVehicle("ICE");
            System.out.println("  [OK] Using default vehicles.");
        }

        System.out.println();

        DisplayUtils.printVehicleInfo(ev.describe(), ice.describe());

        boolean running = true;

        while (running) {
            System.out.println("  ---------------------------------------------");
            System.out.print("  [INPUT] Enter source city       : ");
            String source = scanner.nextLine().trim();

            if (source.equalsIgnoreCase("exit") || source.equalsIgnoreCase("quit")) {
                break;
            }

            if (source.isEmpty()) {
                System.out.println("  [ERROR] City name cannot be empty.");
                continue;
            }

            System.out.print("  [INPUT] Enter destination city  : ");
            String destination = scanner.nextLine().trim();

            if (destination.equalsIgnoreCase("exit") || destination.equalsIgnoreCase("quit")) {
                break;
            }

            if (destination.isEmpty()) {
                System.out.println("  [ERROR] City name cannot be empty.");
                continue;
            }

            if (source.equalsIgnoreCase(destination)) {
                System.out.println("  [ERROR] Source and destination cannot be the same.");
                continue;
            }

            System.out.println();

            try {
                TripSummary summary = analyzer.analyze(source, destination, ev, ice);
                DisplayUtils.printTripSummary(summary);
            } catch (IllegalArgumentException e) {
                System.out.println();
                System.out.println("  [ERROR] " + e.getMessage());
                System.out.println();
            } catch (Exception e) {
                System.out.println();
                System.out.println("  [ERROR] Unexpected error: " + e.getMessage());
                System.out.println();
            }

            System.out.print("  [INPUT] Analyze another route? (yes/no): ");
            String again = scanner.nextLine().trim();

            if (again.equalsIgnoreCase("no") || again.equalsIgnoreCase("n") || again.equalsIgnoreCase("exit")) {
                running = false;
            }

            System.out.println();
        }

        System.out.println("  [EXIT] Thank you for using ChargeFlow V2!");
        System.out.println();
        scanner.close();
    }

    private static EVVehicle readCustomEV(Scanner scanner) {
        System.out.println();
        System.out.println("  [CUSTOM EV] Enter details");
        System.out.print("  [INPUT] EV name                 : ");
        String name = readNonEmptyText(scanner, "Custom EV");

        double battery = readPositiveDouble(scanner, "  [INPUT] Battery capacity (kWh)   : ");
        double range = readPositiveDouble(scanner, "  [INPUT] Full range (km)          : ");
        double consumption = battery / range;

        return VehicleFactory.createCustomEV(name, battery, range, consumption);
    }

    private static ICEVehicle readCustomICE(Scanner scanner) {
        System.out.println();
        System.out.println("  [CUSTOM ICE] Enter details");
        System.out.print("  [INPUT] ICE vehicle name         : ");
        String name = readNonEmptyText(scanner, "Custom ICE");

        System.out.print("  [INPUT] Fuel type (Petrol/Diesel): ");
        String fuelType = readFuelType(scanner);
        double mileage = readPositiveDouble(scanner, "  [INPUT] Mileage (km/L)           : ");
        double tank = readPositiveDouble(scanner, "  [INPUT] Tank capacity (L)        : ");

        return VehicleFactory.createCustomICE(name, fuelType, mileage, tank);
    }

    private static int readChoice(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
            }
            System.out.println("  [ERROR] Please enter a number between " + min + " and " + max + ".");
        }
    }

    private static double readPositiveDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
            }
            System.out.println("  [ERROR] Please enter a positive number.");
        }
    }

    private static String readFuelType(Scanner scanner) {
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("petrol") || input.equalsIgnoreCase("diesel")) {
                return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
            }
            System.out.print("  [ERROR] Enter Petrol or Diesel: ");
        }
    }

    private static String readNonEmptyText(Scanner scanner, String fallback) {
        String value = scanner.nextLine().trim();
        if (value.isEmpty()) {
            return fallback;
        }
        return value;
    }
}
