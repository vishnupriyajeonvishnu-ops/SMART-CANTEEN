package com.dmi.canteen.counter;

import com.dmi.canteen.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CounterService {

    private final CounterRepository counterRepository;

    public CounterService(CounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    @Transactional(readOnly = true)
    public List<Counter> getActiveCounters() {
        return counterRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Counter> getAllCounters() {
        return counterRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Counter getCounterById(Long id) {
        return counterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found with id: " + id));
    }

    @Transactional
    public Counter createCounter(Counter counter) {
        return counterRepository.save(counter);
    }

    @Transactional
    public Counter updateCounter(Long id, Counter updated) {
        Counter existing = getCounterById(id);
        existing.setName(updated.getName());
        existing.setCode(updated.getCode());
        existing.setActive(updated.isActive());
        return counterRepository.save(existing);
    }
}
