package dao.factory;
import cn.edu.sustech.cs307.factory.*;
import cn.edu.sustech.cs307.service.*;
import dao.service.*;

public class ServiceFactoryIns extends ServiceFactory{
    public ServiceFactoryIns() {
        registerService(StudentService.class, new StudentServiceIns());
        registerService(CourseService.class, new CourseServiceIns());
        registerService(DepartmentService.class, new DepartmentServiceIns());
        registerService(InstructorService.class, new InstructorServiceIns());
        registerService(MajorService.class, new MajorServiceIns());
        registerService(SemesterService.class, new SemesterServiceIns());
        registerService(UserService.class, new UserServiceIns());
    }
}
