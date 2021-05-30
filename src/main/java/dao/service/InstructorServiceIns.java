package dao.service;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class InstructorServiceIns implements InstructorService{
    @Override
    public int addInstructor(int userId, String firstName, String lastName) {
        String sql = "insert into instructor values (?, ?);";
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userId);
            preparedStatement.setString(2, firstName + " " + lastName);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
        return userId;
    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {

        return null;
    }
}
