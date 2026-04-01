package com.chargeflow.main;

import com.chargeflow.factory.VehicleFactory;
import com.chargeflow.model.*;
import com.chargeflow.service.TripAnalyzer;
import com.chargeflow.utils.ApiConfig;
import com.chargeflow.utils.DisplayUtils;
import java.util.Scanner;

/**
 * ChargeFlowApp — Interactive entry point.
 * Upgraded: Removed hardcoded route menu. Any city pair works now.
 */
public class ChargeFlowApp {

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");

        Scanner scanner = new Scanner(System.in);
        TripAnalyzer analyzer = new TripAnalyzer();

        DisplayUtils.printBanner();

        // Show API status
        if (ApiConfig.isConfigured()) {
            System.out.println("  [OK] API keys configured - unlimited routes enabled!");
        } else {
            System.out.println("  [WARN] API keys not configured - using offline route database.");
            System.out.println("     To enable unlimited routes, add your keys in ApiConfig.java");
            System.out.println("     -> OpenRouteService: https://openrouteservice.org/dev");
            System.out.println("     -> OpenChargeMap:    https://openchargemap.org/site/develop");
        }
        System.out.println();

        // Create vehicles
        EVVehicle ev = (EVVehicle) VehicleFactory.createVehicle("EV");
        ICEVehicle ice = (ICEVehicle) VehicleFactory.createVehicle("ICE");

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
}
