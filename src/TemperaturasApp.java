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


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

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

    
    private JTabbedPane tabbedPane;
    private JPanel dataPanel;
    private JPanel chartPanelContainer;
    private JTextField startDateField;
    private JTextField endDateField;
    private JButton btnGenerateChart;
    private ChartPanel chartPanel;

    public TemperaturasApp() {
        super("Gestión de Temperaturas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        temperaturas = new ArrayList<>();
        fileChooser = new JFileChooser();

        
        String[] columnNames = { "Ciudad", "Fecha", "Temperatura" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        table = new JTable(tableModel);

        tableModel.addTableModelListener(e -> sincronizarListaTemperaturas());

        
        dataPanel = new JPanel(new BorderLayout());
        dataPanel.add(new JScrollPane(table), BorderLayout.CENTER);

     
        JPanel buttonPanel = new JPanel();
        JButton btnCargar = new JButton("Cargar CSV");
        JButton btnGuardar = new JButton("Guardar");
        JButton btnAgregar = new JButton("Agregar Fila");
        JButton btnEliminar = new JButton("Eliminar Fila");
        JButton btnAnalizar = new JButton("Analizar Fecha");

        buttonPanel.add(btnCargar);
        buttonPanel.add(btnGuardar);
        buttonPanel.add(btnAgregar);
        buttonPanel.add(btnEliminar);
        buttonPanel.add(btnAnalizar);

        dataPanel.add(buttonPanel, BorderLayout.SOUTH);

        
        chartPanelContainer = new JPanel(new BorderLayout());

        
        JPanel dateSelectionPanel = new JPanel();
        dateSelectionPanel.add(new JLabel("Fecha inicio (dd/MM/yyyy):"));
        startDateField = new JTextField(10);
        dateSelectionPanel.add(startDateField);

        dateSelectionPanel.add(new JLabel("Fecha fin (dd/MM/yyyy):"));
        endDateField = new JTextField(10);
        dateSelectionPanel.add(endDateField);

        btnGenerateChart = new JButton("Generar Gráfica");
        dateSelectionPanel.add(btnGenerateChart);

        chartPanelContainer.add(dateSelectionPanel, BorderLayout.NORTH);

        
        chartPanel = new ChartPanel(null);
        chartPanelContainer.add(chartPanel, BorderLayout.CENTER);

        
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Datos", dataPanel);
        tabbedPane.addTab("Gráfica", chartPanelContainer);

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        
        btnCargar.addActionListener(e -> cargarCSV());
        btnGuardar.addActionListener(e -> guardarCSV());
        btnAgregar.addActionListener(e -> agregarFila());
        btnEliminar.addActionListener(e -> eliminarFila());
        btnAnalizar.addActionListener(e -> analizarFecha());

        
        btnGenerateChart.addActionListener(e -> generarGrafica());
    }

    private void cargarCSV() {
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            currentFilePath = fileChooser.getSelectedFile().getPath();
            try {
                List<String> lines = Files.readAllLines(Paths.get(currentFilePath));
                temperaturas.clear();
                tableModel.setRowCount(0);

               
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
                sdf.parse(fecha); 

                
                List<Temperatura> delDia = temperaturas.stream()
                        .filter(t -> t.getFecha().equals(fecha))
                        .collect(Collectors.toList());

                if (delDia.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No hay datos para la fecha " + fecha,
                            "Información", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    
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

   
    private void generarGrafica() {
        String startDateStr = startDateField.getText().trim();
        String endDateStr = endDateField.getText().trim();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);

        try {
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);
            if (startDate.after(endDate)) {
                JOptionPane.showMessageDialog(this, "La fecha de inicio debe ser anterior o igual a la fecha de fin.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            
            List<Temperatura> filtered = temperaturas.stream()
                    .filter(t -> {
                        try {
                            Date fecha = sdf.parse(t.getFecha());
                            return !fecha.before(startDate) && !fecha.after(endDate);
                        } catch (ParseException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay datos disponibles en el rango de fechas seleccionado.",
                        "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

           
            Map<String, Double> avgTempByCity = filtered.stream()
                    .collect(Collectors.groupingBy(Temperatura::getCiudad,
                            Collectors.averagingDouble(Temperatura::getTemperatura)));

           
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            avgTempByCity.forEach((city, avgTemp) -> {
                dataset.addValue(avgTemp, "Temperatura", city);
            });

           
            String chartTitle = String.format("Promedio de Temperatura por Ciudad (%s a %s)", startDateStr, endDateStr);
            JFreeChart barChart = ChartFactory.createBarChart(
                    chartTitle,
                    "Ciudad",
                    "Temperatura (°C)",
                    dataset);

           
            CategoryPlot plot = barChart.getCategoryPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, new Color(255, 100, 100)); // rojo claro

          
            chartPanel.setChart(barChart);

        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Formato de fecha inválido. Use dd/MM/yyyy",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al generar la gráfica: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

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

  
    private void sincronizarListaTemperaturas() {
        temperaturas.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                Object ciudadObj = tableModel.getValueAt(i, 0);
                Object fechaObj = tableModel.getValueAt(i, 1);
                Object tempObj = tableModel.getValueAt(i, 2);
                if (ciudadObj == null || fechaObj == null || tempObj == null) {
                    continue; // Ignorar filas con valores nulos
                }
                String ciudad = ciudadObj.toString().trim();
                String fecha = fechaObj.toString().trim();
                double temp = Double.parseDouble(tempObj.toString().trim());
                temperaturas.add(new Temperatura(ciudad, fecha, temp));
            } catch (Exception e) {
            
            }
        }
    }
}
