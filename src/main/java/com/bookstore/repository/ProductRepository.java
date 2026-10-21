package com.bookstore.repository;

import com.bookstore.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where lower(p.title) like lower(concat('%', :keyword, '%'))
               or lower(p.author) like lower(concat('%', :keyword, '%'))
               or lower(p.isbn) like lower(concat('%', :keyword, '%'))
               or lower(p.description) like lower(concat('%', :keyword, '%'))
            """)
    Page<Product> search(@Param("keyword") String keyword, Pageable pageable);

    /** Pessimistic lock used while reserving stock for an order. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    /** In-stock products from categories the user has bought from, excluding books already bought. */
    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where p.stockQuantity > 0
              and p.category.id in (select oi.product.category.id from OrderItem oi
                                    where oi.order.user.id = :userId and oi.order.status <> com.bookstore.enums.OrderStatus.CANCELLED)
              and p.id not in (select oi.product.id from OrderItem oi where oi.order.user.id = :userId)
            order by p.rating desc nulls last, p.id
            """)
    List<Product> findRecommendedByPurchaseHistory(@Param("userId") Long userId, Pageable pageable);

    /** Fallback: top-rated in-stock books the user has not bought yet. */
    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where p.stockQuantity > 0
              and p.id not in (select oi.product.id from OrderItem oi where oi.order.user.id = :userId)
            order by p.rating desc nulls last, p.id
            """)
    List<Product> findTopRatedNotPurchased(@Param("userId") Long userId, Pageable pageable);
}
