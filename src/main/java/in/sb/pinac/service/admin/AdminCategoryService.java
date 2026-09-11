package in.sb.pinac.service.admin;

import in.sb.pinac.entity.Category;
import in.sb.pinac.entity.Course;
import in.sb.pinac.repository.CategoryRepository;
import in.sb.pinac.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminCategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CourseRepository courseRepository;

    public List<Map<String, Object>> getAllCategories(String search) {
        List<Category> categories = categoryRepository.findAll();
        List<Course> courses = courseRepository.findAll();

        return categories.stream()
                .filter(c -> {
                    if (search == null || search.trim().isEmpty()) return true;
                    String q = search.trim().toLowerCase();
                    boolean matchName = c.getName() != null && c.getName().toLowerCase().contains(q);
                    boolean matchDesc = c.getDescription() != null && c.getDescription().toLowerCase().contains(q);
                    return matchName || matchDesc;
                })
                .map(c -> {
                    long courseCount = courses.stream()
                            .filter(crs -> crs.getCategory() != null && (crs.getCategory().equalsIgnoreCase(c.getName()) || crs.getCategory().equalsIgnoreCase(c.getSlug())))
                            .count();

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("name", c.getName());
                    map.put("slug", c.getSlug());
                    map.put("icon", c.getIcon());
                    map.put("description", c.getDescription());
                    map.put("courseCount", courseCount);
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Category createCategory(Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required.");
        }

        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            String slug = category.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
            category.setSlug(slug);
        }

        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, Category req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found with ID: " + id));

        if (req.getName() != null) category.setName(req.getName().trim());
        if (req.getSlug() != null) category.setSlug(req.getSlug().trim());
        if (req.getIcon() != null) category.setIcon(req.getIcon().trim());
        if (req.getDescription() != null) category.setDescription(req.getDescription().trim());

        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NoSuchElementException("Category not found with ID: " + id);
        }
        categoryRepository.deleteById(id);
    }
}
