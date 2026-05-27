package ra.edu.restapi.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticDTO {
    private String categoryName;
    private Double totalAmount;
}
