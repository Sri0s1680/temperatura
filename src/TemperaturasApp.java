import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TemperaturasApp extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TemperaturasApp app = new TemperaturasApp();
            app.setVisible(true);
        });
    }
    private DefaultTableModel tableModel;
    private JTable table;
    private List<Temperatura> temperaturas;
    private JFileChooser fileChooser;
    private String currentFilePath;

    public TemperaturasApp() {
        super("Gestión de Temperaturas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        temperaturas = new ArrayList<>();
        fileChooser = new JFileChooser();

        // Configurar modelo de tabla
        String[] columnNames = { "Ciudad", "Fecha", "Temperatura" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        table = new JTable(tableModel);

        // Añadir listener para sincronizar lista temperaturas con tabla
        tableModel.addTableModelListener(e -> sincronizarListaTemperaturas());

        // No cargar datos iniciales para que la tabla empiece vacía
        // cargarDatosIniciales(); // Disabled initial data loading

        // Crear componentes
        JButton btnCargar = new JButton("Cargar CSV");
        JButton btnGuardar = new JButton("Guardar");
        JButton btnAgregar = new JButton("Agregar Fila");
        JButton btnEliminar = new JButton("Eliminar Fila");
        JButton btnAnalizar = new JButton("Analizar Fecha");

        // Panel de botones
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnCargar);
        buttonPanel.add(btnGuardar);
        buttonPanel.add(btnAgregar);
        buttonPanel.add(btnEliminar);
        buttonPanel.add(btnAnalizar);

        // Configurar layout
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Eventos
        btnCargar.addActionListener(e -> cargarCSV());
        btnGuardar.addActionListener(e -> guardarCSV());
        btnAgregar.addActionListener(e -> agregarFila());
        btnEliminar.addActionListener(e -> eliminarFila());
        btnAnalizar.addActionListener(e -> analizarFecha());
    }

    private void cargarCSV() {
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            currentFilePath = fileChooser.getSelectedFile().getPath();
            try {
                List<String> lines = Files.readAllLines(Paths.get(currentFilePath));
                temperaturas.clear();
                tableModel.setRowCount(0);

                // Saltar la primera línea si es el encabezado
                int startLine = lines.get(0).trim().equalsIgnoreCase("Ciudad,Fecha,Temperatura") ? 1 : 0;

                for (int i = startLine; i < lines.size(); i++) {
                    String[] parts = lines.get(i).split(",");
                    if (parts.length == 3) {
                        String ciudad = parts[0].trim();
                        String fecha = parts[1].trim();
                        double temp = Double.parseDouble(parts[2].trim());
                        temperaturas.add(new Temperatura(ciudad, fecha, temp));
                        tableModel.addRow(new Object[] { ciudad, fecha, temp });
                    }
                }
                JOptionPane.showMessageDialog(this, "Datos cargados correctamente");
            } catch (IOException | NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Error al cargar el archivo: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void guardarCSV() {
        if (currentFilePath == null) {
            int returnValue = fileChooser.showSaveDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                currentFilePath = fileChooser.getSelectedFile().getPath();
            } else {
                return;
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(currentFilePath))) {
            writer.println("Ciudad,Fecha,Temperatura");
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String ciudad = tableModel.getValueAt(i, 0).toString();
                String fecha = tableModel.getValueAt(i, 1).toString();
                String temp = tableModel.getValueAt(i, 2).toString();
                writer.println(ciudad + "," + fecha + "," + temp);
            }
            JOptionPane.showMessageDialog(this, "Datos guardados correctamente");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarFila() {
        tableModel.addRow(new Object[] { "", "", "" });
        sincronizarListaTemperaturas();
    }

    private void eliminarFila() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            tableModel.removeRow(selectedRow);
            sincronizarListaTemperaturas();
        } else {
            JOptionPane.showMessageDialog(this, "Seleccione una fila para eliminar",
                    "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void analizarFecha() {
        String fecha = JOptionPane.showInputDialog(this, "Ingrese fecha a analizar (dd/MM/yyyy):");
        if (fecha != null && !fecha.trim().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                sdf.parse(fecha); // Validar formato

                // Filtrar temperaturas por fecha
                List<Temperatura> delDia = temperaturas.stream()
                        .filter(t -> t.getFecha().equals(fecha))
                        .collect(Collectors.toList());

                if (delDia.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No hay datos para la fecha " + fecha,
                            "Información", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // Encontrar máximos y mínimos
                    Temperatura max = delDia.stream()
                            .max(Comparator.comparingDouble(Temperatura::getTemperatura))
                            .orElse(null);

                    Temperatura min = delDia.stream()
                            .min(Comparator.comparingDouble(Temperatura::getTemperatura))
                            .orElse(null);

                    String mensaje = String.format(
                            "Análisis para %s:\n\nCiudad más calurosa: %s (%.1f°C)\nCiudad menos calurosa: %s (%.1f°C)",
                            fecha, max.getCiudad(), max.getTemperatura(), min.getCiudad(), min.getTemperatura());

                    JOptionPane.showMessageDialog(this, mensaje, "Resultados", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(this, "Formato de fecha inválido. Use dd/MM/yyyy",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Clase interna para representar los datos de temperatura
    private static class Temperatura {
        private String ciudad;
        private String fecha;
        private double temperatura;

        public Temperatura(String ciudad, String fecha, double temperatura) {
            this.ciudad = ciudad;
            this.fecha = fecha;
            this.temperatura = temperatura;
        }

        public String getCiudad() {
            return ciudad;
        }

        public String getFecha() {
            return fecha;
        }

        public double getTemperatura() {
            return temperatura;
        }
    }

    // Sincronizar la lista de temperaturas con los datos actuales de la tabla
    private void sincronizarListaTemperaturas() {
        temperaturas.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                String ciudad = tableModel.getValueAt(i, 0).toString().trim();
                String fecha = tableModel.getValueAt(i, 1).toString().trim();
                double temp = Double.parseDouble(tableModel.getValueAt(i, 2).toString().trim());
                temperaturas.add(new Temperatura(ciudad, fecha, temp));
            } catch (Exception e) {
                // Ignorar filas con datos inválidos
            }
        }
    }
}
