package com.coffeeshop.controller;

import com.coffeeshop.model.TestCase;
import com.coffeeshop.service.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//defines base url
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }
//matches the frontends post call
    @PostMapping("/run")
    public TestCase runSimulation(@RequestParam(defaultValue = "1") int testId) {
        return simulationService.runSimulation(testId);//calls logic
    }
//matches the frontend get call
    @GetMapping("/history")
    public List<TestCase> getHistory() {
        return simulationService.getHistory();
    }
}
