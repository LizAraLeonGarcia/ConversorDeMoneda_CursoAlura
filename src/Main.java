import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpHeaders;
import java.net.http.HttpTimeoutException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class CurrencyConverter {
    private static final String API_KEY = "58f6a4316ad513c6648db9ef";
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";
    private static final String API_URL = BASE_URL + API_KEY + "/latest/USD";

    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newHttpClient();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        fetchExchangeRates(); // Obtener tasas de cambio al iniciar la aplicación

        boolean running = true;
        while (running) {
            displayMenu();
            int choice = scanner.nextInt();
            scanner.nextLine(); // Limpiar el buffer del scanner después de nextInt()

            switch (choice) {
                case 1:
                    listSupportedCurrencies();
                    break;
                case 2:
                    convertCurrency();
                    break;
                case 3:
                    System.out.println("Saliendo del programa...");
                    running = false;
                    break;
                default:
                    System.out.println("Opción no válida. Por favor, elige una opción del menú.");
            }
        }

        scanner.close();
    }

    public static JsonObject fetchExchangeRates() {
        try {
            // Configuración de la solicitud HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Gestión de la respuesta HttpResponse
            return handleResponse(response);
        } catch (IOException | InterruptedException e) {
            if (e instanceof HttpTimeoutException) {
                System.out.println("Request timed out");
            } else {
                e.printStackTrace();
            }
        }
        return null;
    }

    // Método para gestionar la respuesta HttpResponse
    public static JsonObject handleResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        HttpHeaders headers = response.headers();
        String responseBody = response.body();

        System.out.println("Status Code: " + statusCode);
        System.out.println("Headers: " + headers.map());
        System.out.println("Response Body: " + responseBody); // Imprime la respuesta JSON

        try {
            if (statusCode == 200) {
                JsonObject jsonObject = parseJson(responseBody);

                if (jsonObject != null && jsonObject.has("conversion_rates")) {
                    JsonObject conversionRates = jsonObject.getAsJsonObject("conversion_rates");
                    filterCurrencies(conversionRates);
                    return conversionRates; // Devuelve las tasas de cambio obtenidas
                } else {
                    System.out.println("Response JSON is null or missing 'conversion_rates' object");
                }
            } else {
                System.out.println("Request failed with status code: " + statusCode);
            }
        } catch (JsonSyntaxException e) {
            System.out.println("Error parsing JSON response: " + e.getMessage());
        }
        return null;
    }

    public static void filterCurrencies(JsonObject conversionRates) {
        List<String> currencyCodes = Arrays.asList("ARS", "BOB", "BRL", "CLP", "COP", "USD");

        for (String currency : currencyCodes) {
            if (conversionRates.has(currency)) {
                System.out.println(currency + ": " + conversionRates.get(currency).getAsDouble());
            } else {
                System.out.println(currency + " not found in conversion rates");
            }
        }
    }


    public static JsonObject parseJson(String jsonData) throws JsonSyntaxException {
        try {
            JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);

            if (jsonObject.has("result") && jsonObject.get("result").getAsString().equals("success")) {
                return jsonObject.getAsJsonObject("conversion_rates");
            } else {
                System.out.println("API Error: " + jsonObject.get("error").getAsString());
                return null;
            }
        } catch (JsonSyntaxException e) {
            System.out.println("Error parsing JSON: " + e.getMessage());
            throw e; // Puedes manejar o lanzar la excepción según sea necesario
        }
    }

    public static void listSupportedCurrencies() {
        System.out.println("Monedas soportadas:");
        System.out.println("1. USD - Dólar estadounidense");
        System.out.println("2. EUR - Euro");
        System.out.println("3. GBP - Libra esterlina");
        // Agrega más monedas según sea necesario
    }

    public static void convertCurrency() {
        System.out.println("Ingrese la moneda de origen (ej. USD):");
        String fromCurrency = scanner.nextLine().toUpperCase();

        System.out.println("Ingrese la moneda de destino (ej. EUR):");
        String toCurrency = scanner.nextLine().toUpperCase();

        System.out.println("Ingrese la cantidad a convertir:");
        double amount = scanner.nextDouble();

        if (isValidCurrency(fromCurrency) && isValidCurrency(toCurrency)) {
            JsonObject rates = fetchExchangeRates();
            if (rates != null) {
                performConversion(rates, fromCurrency, toCurrency, amount);
            } else {
                System.out.println("No se pudieron obtener las tasas de cambio. Intente de nuevo más tarde.");
            }
        } else {
            System.out.println("Moneda no válida. Por favor, elija una moneda de la lista.");
        }
    }

    public static void performConversion(JsonObject rates, String fromCurrency, String toCurrency, double amount) {
        if (rates.has(fromCurrency) && rates.has(toCurrency)) {
            double fromRate = rates.get(fromCurrency).getAsDouble();
            double toRate = rates.get(toCurrency).getAsDouble();

            double convertedAmount = amount * (toRate / fromRate);

            System.out.println(amount + " " + fromCurrency + " = " + convertedAmount + " " + toCurrency);
        } else {
            System.out.println("Una o ambas monedas no se encuentran en las tasas de cambio actuales.");
        }
    }

    public static boolean isValidCurrency(String currencyCode) {
        List<String> supportedCurrencies = Arrays.asList("USD", "EUR", "GBP");
        return supportedCurrencies.contains(currencyCode);
    }

    public static void displayMenu() {
        System.out.println("\nBienvenido al Conversor de Monedas");
        System.out.println("Selecciona una opción:");
        System.out.println("1. Ver monedas soportadas");
        System.out.println("2. Convertir moneda");
        System.out.println("3. Salir");
        System.out.print("Opción: ");
    }
}
