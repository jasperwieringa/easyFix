package com.main.easyFix.customer;

import com.main.easyFix.appointment.Appointment;
import com.main.easyFix.security.PermissionValidator;
import com.main.easyFix.usedpart.UsedPartService;
import com.main.easyFix.utils.EmailValidator;
import javassist.NotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerService {
  private final static String CLIENT_NOT_FOUND_MSG = "Customer with %s: %s, not found";
  private final CustomerRepository customerRepository;
  private final UsedPartService usedPartService;
  private final EmailValidator emailValidator;

  public Customer loadCustomerById(int id) throws UsernameNotFoundException {
    return customerRepository.findById(id).orElseThrow(() ->
      new UsernameNotFoundException(String.format(CLIENT_NOT_FOUND_MSG, "id", id)));
  }

  public Object listAllCustomers() {
    return customerRepository.findAll();
  }

  public void add(Authentication authentication, Customer customer) throws IllegalAccessException {
    if (!PermissionValidator.isAdmin(authentication)) {
      throw new IllegalAccessException("Permission denied");
    }

    if (!emailValidator.test(customer.getEmail())) {
      throw new IllegalStateException("Invalid email address");
    }

    boolean emailInUse = customerRepository.findByEmail(customer.getEmail()).isPresent();
    if (emailInUse) {
      throw new IllegalStateException("A customer with this email already exists");
    }

    customerRepository.save(customer);
  }

  public void update(Customer customer) {
    customerRepository.save(customer);
  }

  public void remove(Authentication authentication, int customer_id) throws IllegalAccessException, NotFoundException {
    if (!PermissionValidator.isAdmin(authentication)) {
      throw new IllegalAccessException("Permission denied");
    }

    Customer customer = loadCustomerById(customer_id);
    Appointment appointment = customer.getAppointment();

    // Remove the associated used parts from the appointment
    usedPartService.removeAll(authentication, appointment);

    // Finally delete the customer
    customerRepository.delete(customer);
  }
}
