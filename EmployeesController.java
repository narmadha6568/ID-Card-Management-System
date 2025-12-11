package com.Id;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;




@Controller
@RequestMapping("/employees")
public class EmployeesController {

    @Autowired
    private EmployeesRepository repo;

    @GetMapping
    public String showEmployeeList(Model model) {
        model.addAttribute("employees", repo.findAll());
        return "employees/index";
    }

    @GetMapping("/create")    // --> to display create page 
    public String showCreatePage(Model model) {
        //EmployeeDto dto = new EmployeeDto();
        //dto.setJoinedAt(LocalDate.now());  // ✅ Set default joined date
        //model.addAttribute("employeeDto", dto);
    	model.addAttribute("employeeDto", new EmployeeDto());
        return "employees/CreateEmployee";
    }

    @PostMapping("/create")  //  --> to validate the given details and store on DB
    public String createEmployee(@Validated @ModelAttribute EmployeeDto employeeDto, BindingResult result) {
        MultipartFile imageFile = employeeDto.getImageFile();
        String name=employeeDto.getName();
        if (imageFile.isEmpty()) {
            result.rejectValue("imageFile", null, "The image file is required");
        }
      if(name.isEmpty()) {
    	  result.rejectValue("name",null,"this is required field");
      }
        if (result.hasErrors()) {  
            return "employees/CreateEmployee";
        }

        String storageFileName = new Date().getTime() + "_" + imageFile.getOriginalFilename();
       
        Path uploadPath = Paths.get("public/Images/");

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (InputStream inputStream = imageFile.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }

            // Convert joinedAt to LocalDate from the employeeDto (Ensure it's not null)
            LocalDate joinedAt = employeeDto.getJoinedAt() != null ? employeeDto.getJoinedAt() : LocalDate.now();  //-> ternary operator
                      
            
            Employee employee = new Employee();
            employee.setName(employeeDto.getName());
            employee.setCity(employeeDto.getCity());
            employee.setDesignation(employeeDto.getDesignation());
            employee.setContactDetails(employeeDto.getContactDetails());
            employee.setAbout(employeeDto.getAbout());
            employee.setJoinedAt(LocalDate.now());    // Set the LocalDate value here
            employee.setImageFileName(storageFileName);

            repo.save(employee);

        } catch (IOException ex) {
            ex.printStackTrace();  // -> 
        }

        return "redirect:/employees";
    }

    @GetMapping("/edit")
    //localhost:8082/employees/edit?id=2
    public String showEditPage(@RequestParam("id") int id, Model model) {
        Optional<Employee> employeeOpt = repo.findById(id);

        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            EmployeeDto employeeDto = new EmployeeDto();

            // Set fields
            employeeDto.setId(employee.getId());
            employeeDto.setName(employee.getName());
            employeeDto.setCity(employee.getCity());
            employeeDto.setDesignation(employee.getDesignation());
            employeeDto.setContactDetails(employee.getContactDetails());
            employeeDto.setAbout(employee.getAbout());
            employeeDto.setImageFileName(employee.getImageFileName());
            employeeDto.setJoinedAt(employee.getJoinedAt()); // Set LocalDate

            model.addAttribute("employeeDto", employeeDto);
            return "employees/EditEmployee";
        }

        return "redirect:/employees";
    }

    @PostMapping("/edit")
    public String updateEmployee(@RequestParam("id") int id, @Validated @ModelAttribute EmployeeDto employeeDto,
                                 BindingResult result, Model model) {

        if (result.hasErrors()) {
            model.addAttribute("id", id);
            return "employees/EditEmployee";
        }

        Optional<Employee> employeeOpt = repo.findById(id);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();

            employee.setName(employeeDto.getName());
            employee.setCity(employeeDto.getCity());
            employee.setDesignation(employeeDto.getDesignation());
            employee.setContactDetails(employeeDto.getContactDetails());
            employee.setAbout(employeeDto.getAbout());

            // Handle image upload
            MultipartFile newImageFile = employeeDto.getImageFile();

            if (newImageFile != null && !newImageFile.isEmpty()) {
                // Delete old image
                String oldImageFileName = employee.getImageFileName();
                Path oldImagePath = Paths.get("public/Images/", oldImageFileName);
                try {
                    Files.deleteIfExists(oldImagePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Save new image
                String newImageFileName = new Date().getTime() + "_" + newImageFile.getOriginalFilename();
                Path uploadPath = Paths.get("public/Images/");

                try {
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    try (InputStream inputStream = newImageFile.getInputStream()) {
                        Files.copy(inputStream, uploadPath.resolve(newImageFileName), StandardCopyOption.REPLACE_EXISTING);
                    }

                    employee.setImageFileName(newImageFileName);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            // Update joinedAt with the LocalDate from employeeDto
            employee.setJoinedAt(employeeDto.getJoinedAt());

            repo.save(employee);
        }

        return "redirect:/employees";
    }

    @GetMapping("/delete")
    //localhost:8082/employees/delete?id=1
    public String deleteEmployee(@RequestParam("id") int id) {
        Optional<Employee> employeeOpt = repo.findById(id);

        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            Path imagePath = Paths.get("public/Images/", employee.getImageFileName());

            try {
                Files.deleteIfExists(imagePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            repo.delete(employee);
        }

        return "redirect:/employees";
    }
}