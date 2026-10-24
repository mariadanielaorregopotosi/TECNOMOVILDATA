
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TecnoMovilData {

    // 1. Modelo inmutable de un registro de transporte.
    record Registro(
            String idUsuario,
            String ruta,
            String estacion,
            String accion,
            LocalDateTime timestamp) {

        Registro {
            if (idUsuario == null || ruta == null ||
                estacion == null || accion == null ||
                timestamp == null) {
                throw new IllegalArgumentException(
                        "Los campos del registro son obligatorios.");
            }

            if (!accion.equals("entrada") &&
                !accion.equals("salida")) {
                throw new IllegalArgumentException(
                        "La accion debe ser entrada o salida.");
            }
        }
    }

    // 2. Genera los datos de prueba sin modificar listas existentes.
    static List<Registro> generarDatos() {
        LocalDateTime inicio =
                LocalDateTime.of(2026, 10, 24, 5, 0);

        List<Registro> muestra = List.of(
            new Registro("U01", "R1", "Centro", "entrada",
                    inicio.plusMinutes(0)),
            new Registro("U01", "R1", "Universidad", "salida",
                    inicio.plusMinutes(12)),
            new Registro("U01", "R2", "Hospital", "entrada",
                    inicio.plusMinutes(25)),
            new Registro("U02", "R1", "Centro", "entrada",
                    inicio.plusMinutes(30)),
            new Registro("U02", "R1", "Universidad", "salida",
                    inicio.plusMinutes(43)),
            new Registro("U03", "R2", "Hospital", "entrada",
                    inicio.plusMinutes(60)),
            new Registro("U03", "R2", "Terminal", "salida",
                    inicio.plusMinutes(78)),
            new Registro("U04", "R3", "Centro", "entrada",
                    inicio.plusHours(1)),
            new Registro("U04", "R3", "Terminal", "salida",
                    inicio.plusHours(1).plusMinutes(20)),
            new Registro("U05", "R1", "Centro", "entrada",
                    inicio.plusHours(2)),
            new Registro("U05", "R1", "Universidad", "salida",
                    inicio.plusHours(2).plusMinutes(15)),
            new Registro("U06", "R3", "Hospital", "entrada",
                    inicio.plusHours(3))
        );

        // Repetimos el conjunto para simular un volumen mayor.
        // El identificador del usuario se conserva dentro de
        // cada bloque para mantener los viajes de prueba.
        List<Registro> datosSimulados = IntStream.range(0, 100)
                .boxed()
                .flatMap(bloque -> muestra.stream()
                        .map(r -> new Registro(
                                r.idUsuario() + "_" + bloque,
                                r.ruta(),
                                r.estacion(),
                                r.accion(),
                                r.timestamp().plusDays(bloque))))
                .collect(Collectors.toList());

        return List.copyOf(datosSimulados);
    }

    // a) Afluencia: contar las entradas por estación.
    static Map<String, Long> afluenciaPorEstacion(
            List<Registro> registros) {

        return registros.stream()
                .filter(r -> r.accion().equals("entrada"))
                .collect(Collectors.groupingBy(
                        Registro::estacion,
                        Collectors.counting()));
    }

    // b) Horas pico: contar las entradas por hora del día.
    static Map<Integer, Long> afluenciaPorHora(
            List<Registro> registros) {

        return registros.stream()
                .filter(r -> r.accion().equals("entrada"))
                .collect(Collectors.groupingBy(
                        r -> r.timestamp().getHour(),
                        Collectors.counting()));
    }

    // c) Rutas más utilizadas: contar entradas por ruta.
    static Map<String, Long> rutasMasUtilizadas(
            List<Registro> registros) {

        return registros.stream()
                .filter(r -> r.accion().equals("entrada"))
                .collect(Collectors.groupingBy(
                        Registro::ruta,
                        Collectors.counting()));
    }

    // d) Estaciones visitadas por cada usuario, en orden temporal.
    static Map<String, List<String>> patronesDeViaje(
            List<Registro> registros) {

        return registros.stream()
                .sorted(Comparator.comparing(Registro::timestamp))
                .filter(r -> r.accion().equals("entrada"))
                .collect(Collectors.groupingBy(
                        Registro::idUsuario,
                        Collectors.collectingAndThen(
                                Collectors.mapping(
                                        Registro::estacion,
                                        Collectors.toList()),
                                List::copyOf)));
    }

    // e) Tiempo promedio entre estaciones consecutivas.
    // Se toman registros consecutivos de cada usuario ordenados
    // por fecha. Solo se mide cuando cambia la estación.
    static double tiempoPromedioEntreEstaciones(
            List<Registro> registros) {

        Map<String, List<Registro>> porUsuario =
                registros.stream().collect(Collectors.groupingBy(
                        Registro::idUsuario));

        return porUsuario.values().stream()
                .flatMap(lista -> {
                    List<Registro> ordenados = lista.stream()
                            .sorted(Comparator.comparing(
                                    Registro::timestamp))
                            .toList();

                    return IntStream.range(1, ordenados.size())
                            .mapToObj(i -> {
                                Registro anterior =
                                        ordenados.get(i - 1);
                                Registro actual = ordenados.get(i);

                                if (anterior.estacion().equals(
                                        actual.estacion())) {
                                    return null;
                                }

                                return Duration.between(
                                        anterior.timestamp(),
                                        actual.timestamp()).toMinutes();
                            });
                })
                .filter(minutos -> minutos != null)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    // f) Detectar rutas críticas usando un umbral simulado.
    static List<String> detectarRutasCriticas(
            Map<String, Long> usoPorRuta, long umbral) {

        return usoPorRuta.entrySet().stream()
                .filter(e -> e.getValue() > umbral)
                .sorted(Map.Entry.<String, Long>comparingByValue()
                        .reversed())
                .map(e -> e.getKey() + " (" + e.getValue()
                        + " entradas)")
                .toList();
    }

    // Composición: prepara y presenta el análisis completo.
    static void ejecutarAnalisis(List<Registro> registros) {
        Map<String, Long> afluencia =
                afluenciaPorEstacion(registros);

        Map<Integer, Long> horas =
                afluenciaPorHora(registros);

        Map<String, Long> rutas =
                rutasMasUtilizadas(registros);

        Map<String, List<String>> patrones =
                patronesDeViaje(registros);

        double promedio =
                tiempoPromedioEntreEstaciones(registros);

        List<String> criticas =
                detectarRutasCriticas(rutas, 250);

        System.out.println("=== TECNOMOVIL DATA ===");
        System.out.println("Total de registros: " + registros.size());

        System.out.println("\nA) Afluencia por estación:");
        afluencia.forEach((estacion, cantidad) ->
                System.out.println(estacion + ": " + cantidad));

        System.out.println("\nB) Entradas por hora:");
        horas.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.println(
                        e.getKey() + ":00 = " + e.getValue()));

        System.out.println("\nC) Rutas más utilizadas:");
        rutas.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue()
                        .reversed())
                .forEach(e -> System.out.println(
                        e.getKey() + ": " + e.getValue()));

        System.out.println("\nD) Patrones de viaje:");
        patrones.forEach((usuario, estaciones) ->
                System.out.println(usuario + ": " + estaciones));

        System.out.printf(
                "\nE) Tiempo promedio entre estaciones: %.2f minutos%n",
                promedio);

        System.out.println("\nF) Rutas críticas (umbral > 250):");
        if (criticas.isEmpty()) {
            System.out.println("No se detectaron rutas críticas.");
        } else {
            criticas.forEach(System.out::println);
        }
    }

    public static void main(String[] args) {
        List<Registro> registros = generarDatos();

        ejecutarAnalisis(registros);
    }
}
