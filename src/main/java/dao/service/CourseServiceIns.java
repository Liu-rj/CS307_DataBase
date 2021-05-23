package dao.service;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.dto.CourseSectionClass;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.service.*;
import cn.edu.sustech.cs307.exception.*;

import javax.annotation.Nullable;
import java.time.DayOfWeek;
import java.util.*;

import java.sql.*;


public class CourseServiceIns implements CourseService {
    public static void parsePre(Queue<List<Prerequisite>> presQueue, Set<List<Prerequisite>> resultSet) {
        List<Prerequisite> listTemp;
        Prerequisite preTemp = null;
        while (!presQueue.isEmpty()) {
            listTemp = presQueue.poll();
            for (Prerequisite p : listTemp) {
                if (p instanceof AndPrerequisite || p instanceof OrPrerequisite) {
                    preTemp = p;
                    listTemp.remove(p);
                    break;
                }
            }
            if (preTemp == null) {
                resultSet.add(listTemp);
                continue;
            }
            if (preTemp instanceof AndPrerequisite) {
                listTemp.addAll(((AndPrerequisite) preTemp).terms);
                presQueue.offer(listTemp);
            } else {
                for (Prerequisite p : ((OrPrerequisite) preTemp).terms) {
                    List<Prerequisite> newList = new ArrayList<>(listTemp);
                    newList.add(p);
                    presQueue.offer(newList);
                }
            }
        }
    }

    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite coursePrerequisite) {
        String sql = "insert into course values (default, ?, ?, ?, ?, ?)";
        String relation = "insert into prerequisite values (?, ?, ?)";
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // add to course table not handling pre
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, courseId);
            preparedStatement.setString(2, courseName);
            preparedStatement.setInt(3, credit);
            preparedStatement.setInt(4, classHour);
            preparedStatement.setBoolean(5, grading == Course.CourseGrading.HUNDRED_MARK_SCORE);
            preparedStatement.execute();

            // return val for this interface
            preparedStatement = connection.prepareStatement("select currval(pg_get_serial_sequence('course', 'id_serial'));");
            preparedStatement.execute();
            int curVal = preparedStatement.getResultSet().getInt(1);

            // parse and add relation prerequisite
            if (coursePrerequisite != null) {
                Queue<List<Prerequisite>> presQueue = new LinkedList<>();
                Set<List<Prerequisite>> resultSet = new HashSet<>(); // result and group
                presQueue.add(new ArrayList<>());
                presQueue.peek().add(coursePrerequisite);
                parsePre(presQueue, resultSet);
                int groupNum = 1, id_serial;
                for (List<Prerequisite> l : resultSet) {
                    for (Prerequisite p : l) {
                        // query id_serial corresponding to courseId
                        preparedStatement = connection.prepareStatement("select id_serial from course where id = " + ((CoursePrerequisite) p).courseID);
                        preparedStatement.execute();
                        id_serial = preparedStatement.getResultSet().getInt(1);

                        // insert record(curVal, id_serial, groupNum) into pre table
                        preparedStatement = connection.prepareStatement(relation);
                        preparedStatement.setInt(1, curVal);
                        preparedStatement.setInt(2, id_serial);
                        preparedStatement.setInt(3, groupNum);
                        preparedStatement.execute();
                        groupNum++;
                    }
                }
            }
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        String courseSection = "insert into coursesection values (default, ?,?,?,?)";
        String courseSemester = "insert into course_semester values (?,?)";
        PreparedStatement preparedStatement;
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();

            // query id_serial according to courseId
            preparedStatement = connection.prepareStatement("select id_serial from course where id = " + courseId);
            preparedStatement.execute();
            int cid = preparedStatement.getResultSet().getInt(1);

            // insert record into courseSection table
            preparedStatement = connection.prepareStatement(courseSection);
            preparedStatement.setInt(1, cid);
            preparedStatement.setString(2, sectionName);
            preparedStatement.setInt(3, totalCapacity);
            preparedStatement.setInt(4, totalCapacity);
            preparedStatement.execute();

            // insert record into section_semester
            preparedStatement = connection.prepareStatement(courseSemester);
            preparedStatement.setInt(1, cid);
            preparedStatement.setInt(2, semesterId);
            preparedStatement.execute();

            // return val for this interface
            preparedStatement = connection.prepareStatement("select currval(pg_get_serial_sequence('coursesection', 'id_serial'));");
            preparedStatement.execute();
            int returnVal = preparedStatement.getResultSet().getInt(1);

            // close connection
            connection.close();
            preparedStatement.close();
            return returnVal;
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, List<Short> weekList, short classStart, short classEnd, String location) {

        return 0;
    }

    @Override
    public void removeCourse(String courseId) {
        String sql = "delete from Course where Course.id = ?";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, courseId);
            preparedStatement.execute();
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void removeCourseSection(int sectionId) {
        String sql = "delete from CourseSection where CourseSection.id = ?";
        try {
            Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, sectionId);
            preparedStatement.execute();
            connection.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeCourseSectionClass(int classId) {

    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        return null;
    }

    @Override
    public Course getCourseBySection(int sectionId) {
        return null;
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        return null;
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        return null;
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        return null;
    }
}
