package com.dmi.canteen.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyMenuRepository extends JpaRepository<DailyMenu, Long> {

    List<DailyMenu> findByMenuDate(LocalDate menuDate);

    @Query("SELECT dm FROM DailyMenu dm WHERE dm.menuDate = :menuDate AND dm.available = true AND dm.foodItem.active = true")
    List<DailyMenu> findActiveItemsForDate(@Param("menuDate") LocalDate menuDate);

    Optional<DailyMenu> findByFoodItemIdAndMenuDate(Long foodItemId, LocalDate menuDate);
}
