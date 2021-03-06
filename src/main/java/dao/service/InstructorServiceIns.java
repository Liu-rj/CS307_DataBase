package dao.service;
import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InstructorServiceIns implements InstructorService{
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        String sql = "insert into instructor values (?, ?);";
        PreparedStatement preparedStatement;
        try {
            String fullName;
            if (firstName.charAt(0) > 'z') {
                fullName = firstName + lastName;
            } else {
                fullName = firstName + " " + lastName;
            }

            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, userId);
            preparedStatement.setString(2, fullName);
            preparedStatement.execute();

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        //TODO:这个方法报红的地方需要进行修改
        String sel = "select *\n" +
                "from section_semester ss\n" +
                "         join ins_section us on ss.section_id = us.section_id\n" +
                "where us.ins_id = ?\n" +
                "  and ss.semester_id = ?;";

//        String sel = "select *\n" +
//                "from section_semester ss\n" +
//                "         join user_section us on ss.section_id = us.section_id\n" +
//                "         join section s on s.section_id = ss.section_id\n" +
//                "where us.user_id = ?\n" +
//                "  and ss.semester_id = ?;";
        List<CourseSection> sections = new ArrayList<>();
        CourseSection section;
        ResultSet resultSet;
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(sel);
            preparedStatement.setInt(1, instructorId);
            preparedStatement.setInt(2, semesterId);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                section = new CourseSection();
                section.id = resultSet.getInt("ss.section_id");
                section.name = resultSet.getString("name");
                section.totalCapacity = resultSet.getInt("total_capacity");
                section.leftCapacity = resultSet.getInt("left_capacity");
                sections.add(section);
            }

            // close connection
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sections;
    }
}
