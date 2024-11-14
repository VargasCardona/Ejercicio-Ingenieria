package workflows;

import entitymanager.EntityManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import models.Persona;
import models.Solicitud;

public class ProcesadorCsv {

    EntityManager em = new EntityManager();

    public void processAllCsvFiles() {
        try (Stream<Path> paths = Files.walk(Paths.get("resources/SolicitudesRecibidas"))) {
            paths.filter(Files::isRegularFile) // Only regular files
                    .filter(path -> path.toString().endsWith(".csv")) // Only CSV files
                    .forEach(this::processSingleCsvFile); // Process each file one by one
        } catch (IOException e) {
            System.out.println("Error accessing files: " + e.getMessage());
        }
    }

    public void processSingleCsvFile(Path csvFile) {
        List<Solicitud> listaSolicitudes = em.cargarDesdeCSV(csvFile.toString(), Solicitud.class);
        List<Solicitud> listaSolicitudesRechazadas = em.cargarDesdeCSV(csvFile.toString(), Solicitud.class);
        for (int i = 0; i < listaSolicitudes.size(); i++) {
            Solicitud aux = listaSolicitudes.get(i);
            if((aux.isEnListaNegra() && aux.getFechaIngresoListaNegra().isBefore(LocalDate.now().minusDays(180))) || aux.isPrePensionado()){
                listaSolicitudesRechazadas.add(aux);
            }
            
            if(!(aux.getInstitucionPublica().equals("Armada") || aux.getInstitucionPublica().equals("Inpec") || 
                    aux.getInstitucionPublica().equals("Policia") || aux.getInstitucionPublica().equals("MinSalud")
                    || aux.getInstitucionPublica().equals("MinInterior"))){
                if((aux.getCiudadNacimiento().equals("Bogotá") && aux.getCiudadResidencia().equals("Bogotá")) ||
                    (aux.getCiudadNacimiento().equals("Medellín") && aux.getCiudadResidencia().equals("Medellín")) ||
                    (aux.getCiudadNacimiento().equals("Cali") && aux.getCiudadResidencia().equals("Cali")) ||
                    (aux.getCiudadNacimiento().endsWith("tán"))){
                }else{
                    if((aux.getSexo() && aux.getFechaNacimiento().getYear() < 1962) || (!aux.getSexo() && aux.getFechaNacimiento().getYear() < 1967)){
                        listaSolicitudesRechazadas.add(aux);
                    }else{
                        if((aux.getFondoOrigen().equals("Porvenir") && aux.getSemanasCotizadas() > 800)  ||
                                (aux.getFondoOrigen().equals("Protección") && aux.getSemanasCotizadas() > 590) ||
                                (aux.getFondoOrigen().equals("Colfondos") && aux.getSemanasCotizadas() > 300) ||
                                (aux.getFondoOrigen().equals("Old Mutual") && aux.getSemanasCotizadas() > 100) ){
                            listaSolicitudesRechazadas.add(aux);
                        }
                    }
                }
            }else{
                if((aux.getInstitucionPublica().equals("Armada") && !aux.isCondecoracion()) || 
                        (aux.getInstitucionPublica().equals("Inpec") && (aux.getNumHijos() == 0) && !aux.isCondecoracion()) || 
                    (aux.getInstitucionPublica().equals("Policia") && aux.isTieneFamiliaEnPolicia().equals("Menor"))){
                    //Segun el diagrama dice que se debe procesar como civil, que hago en este caso?
                }else if((aux.getInstitucionPublica().equals("MinSalud") || 
                        (aux.getInstitucionPublica().equals("MinInterior")) && aux.isObservacionDisciplinaria())){
                        aux.setFechaIngresoListaNegra(LocalDate.now());
                        aux.setEnListaNegra(true);
                        listaSolicitudesRechazadas.add(aux);
                        }
            } 
        } 
        
        System.out.println(listaSolicitudes);
        System.out.println(listaSolicitudesRechazadas);
    }

   

}
