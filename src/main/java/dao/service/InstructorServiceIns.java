package dao.service;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.service.*;

import java.util.List;

public class InstructorServiceIns implements InstructorService{
    @Override
    public int addInstructor(int userId, String firstName, String lastName) {
        return 0;
    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        return null;
    }
}
