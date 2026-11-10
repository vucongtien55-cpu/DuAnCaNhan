package org.example.quanlychitieu.controller;

import org.example.quanlychitieu.entity.SavingsJar;
import org.example.quanlychitieu.service.SavingsJarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/savings-jars")
@CrossOrigin(origins = "*")
public class SavingsJarController {

    private final SavingsJarService savingsJarService;

    @Autowired
    public SavingsJarController(SavingsJarService savingsJarService) {
        this.savingsJarService = savingsJarService;
    }

    @GetMapping
    public ResponseEntity<List<SavingsJar>> getSavingsJars(@RequestParam String email) {
        return ResponseEntity.ok(savingsJarService.getSavingsJarsByUser(email));
    }

    @PostMapping
    public ResponseEntity<SavingsJar> saveSavingsJar(@RequestBody SavingsJar jar) {
        return ResponseEntity.ok(savingsJarService.saveOrUpdateSavingsJar(jar));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSavingsJar(@PathVariable Long id, @RequestParam String email) {
        try {
            savingsJarService.deleteSavingsJar(id, email);
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncSavingsJars(@RequestParam String email, @RequestBody List<Map<String, Object>> jarsData) {
        try {
            List<SavingsJar> existing = savingsJarService.getSavingsJarsByUser(email);
            for (SavingsJar jar : existing) {
                savingsJarService.deleteSavingsJar(jar.getId(), email);
            }
            for (Map<String, Object> data : jarsData) {
                SavingsJar jar = new SavingsJar();
                jar.setUserEmail(email);
                jar.setName((String) data.get("name"));

                Object targetObj = data.get("targetAmount");
                if (targetObj == null) {
                    targetObj = data.get("target");
                }
                jar.setTargetAmount(Double.parseDouble(targetObj.toString()));

                Object currentObj = data.get("currentAmount");
                if (currentObj == null) {
                    currentObj = data.get("current");
                }
                jar.setCurrentAmount(Double.parseDouble(currentObj.toString()));

                jar.setColor((String) data.get("color"));
                jar.setIcon((String) data.get("icon"));

                savingsJarService.saveOrUpdateSavingsJar(jar);
            }
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
