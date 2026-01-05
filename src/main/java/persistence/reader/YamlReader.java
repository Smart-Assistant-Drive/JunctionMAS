package persistence.reader;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import persistence.model.Junction;
import persistence.model.JunctionList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class YamlReader {

    public static void main(String[] args) {

    }

    public static List<Junction> getJunctions(String fileName){
        Yaml yaml = new Yaml();
        List<Junction> junctionsList = new ArrayList<>();
        try {
            // Carica il file dalla cartella del progetto o dal classpath
            File yamlFile = new File("src/main/resources/" + fileName + ".yml");

            // Carica l'intero file YAML nell'oggetto AppConfig
            FileReader reader = new FileReader(yamlFile);
            JunctionList junctions = yaml.loadAs(reader, JunctionList.class);
            reader.close();

            // Stampa i risultati
            for (Junction j : junctions.getJunctions()) {
                System.out.println("Trovata: " + j);
                junctionsList.add(j);
            }

        } catch (FileNotFoundException e) {
            System.err.println("File config.yml non trovato!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return junctionsList;
    }
}