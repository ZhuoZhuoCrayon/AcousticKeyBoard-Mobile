package tech.oom.idealrecorderdemo.dto;

import java.util.List;

public class ModelInstDebugDto {
    public int dataset_id;
    public String algorithm;
    public String label;
    public List<Short> signal;

    public ModelInstDebugDto(int dataset_id, String algorithm, String label, List<Short> signal) {
        this.dataset_id = dataset_id;
        this.algorithm = algorithm;
        this.label = label;
        this.signal = signal;
    }
}
