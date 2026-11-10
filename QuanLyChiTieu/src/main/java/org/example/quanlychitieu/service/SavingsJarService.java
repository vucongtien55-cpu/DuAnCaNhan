package org.example.quanlychitieu.service;

import org.example.quanlychitieu.entity.SavingsJar;
import org.example.quanlychitieu.repository.SavingsJarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SavingsJarService {

    private final SavingsJarRepository savingsJarRepository;

    @Autowired
    public SavingsJarService(SavingsJarRepository savingsJarRepository) {
        this.savingsJarRepository = savingsJarRepository;
    }

    public List<SavingsJar> getSavingsJarsByUser(String email) {
        return savingsJarRepository.findByUserEmail(email);
    }

    public SavingsJar saveOrUpdateSavingsJar(SavingsJar jar) {
        Optional<SavingsJar> existing = savingsJarRepository.findByUserEmailAndName(jar.getUserEmail(), jar.getName());
        if (existing.isPresent()) {
            SavingsJar current = existing.get();
            current.setTargetAmount(jar.getTargetAmount());
            current.setCurrentAmount(jar.getCurrentAmount());
            current.setColor(jar.getColor());
            current.setIcon(jar.getIcon());
            return savingsJarRepository.save(current);
        }
        return savingsJarRepository.save(jar);
    }

    public void deleteSavingsJar(Long id, String email) {
        SavingsJar jar = savingsJarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Savings jar not found"));
        if (!jar.getUserEmail().equals(email)) {
            throw new SecurityException("Unauthorized access to delete this savings jar");
        }
        savingsJarRepository.delete(jar);
    }
}
