/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gini.scheduling;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.gini.scheduling.domain.Staff;
import com.gini.scheduling.domain.Shift;
import com.gini.scheduling.domain.Timeslot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;

import com.gini.scheduling.persistence.StaffRepository;
import com.gini.scheduling.persistence.ShiftRepository;
import com.gini.scheduling.persistence.TimeslotRepository;

@SpringBootApplication
public class TimeTableSpringBootApp extends SpringBootServletInitializer {

	@Override
    protected SpringApplicationBuilder configure(
      SpringApplicationBuilder builder) {
        return builder.sources(TimeTableSpringBootApp.class);
    }
	
	public static void main(String[] args) {
		SpringApplication.run(TimeTableSpringBootApp.class, args);
	}

	@Value("${timeTable.demoData:SMALL}")
	private DemoData demoData;

	@Bean
	public CommandLineRunner demoData(TimeslotRepository timeslotRepository, ShiftRepository shiftRepository,
			StaffRepository staffRepository) {
		return (args) -> {
			if (demoData == DemoData.NONE) {
				return;
			}
			timeslotRepository.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(00, 00), LocalTime.of(8, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(8, 00), LocalTime.of(16, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(12, 00), LocalTime.of(20, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(16, 00), LocalTime.of(00, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(00, 00), LocalTime.of(8, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(8, 00), LocalTime.of(16, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(12, 00), LocalTime.of(20, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(16, 00), LocalTime.of(00, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.WEDNESDAY, LocalTime.of(00, 00), LocalTime.of(8, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.WEDNESDAY, LocalTime.of(8, 00), LocalTime.of(16, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.WEDNESDAY, LocalTime.of(12, 00), LocalTime.of(20, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.WEDNESDAY, LocalTime.of(16, 00), LocalTime.of(00, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.THURSDAY, LocalTime.of(00, 00), LocalTime.of(8, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.THURSDAY, LocalTime.of(8, 00), LocalTime.of(16, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.THURSDAY, LocalTime.of(12, 00), LocalTime.of(20, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.THURSDAY, LocalTime.of(16, 00), LocalTime.of(00, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.FRIDAY, LocalTime.of(00, 00), LocalTime.of(8, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.FRIDAY, LocalTime.of(8, 00), LocalTime.of(16, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.FRIDAY, LocalTime.of(12, 00), LocalTime.of(20, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.FRIDAY, LocalTime.of(16, 00), LocalTime.of(00, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.SATURDAY, LocalTime.of(00, 00), LocalTime.of(8, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.SATURDAY, LocalTime.of(8, 00), LocalTime.of(16, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.SATURDAY, LocalTime.of(12, 00), LocalTime.of(20, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.SATURDAY, LocalTime.of(16, 00), LocalTime.of(00, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.SUNDAY, LocalTime.of(00, 00), LocalTime.of(8, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.SUNDAY, LocalTime.of(8, 00), LocalTime.of(16, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.SUNDAY, LocalTime.of(12, 00), LocalTime.of(20, 00)));
			timeslotRepository.save(new Timeslot(DayOfWeek.SUNDAY, LocalTime.of(16, 00), LocalTime.of(00, 00)));
			

			shiftRepository.save(new Shift("D6"));
			shiftRepository.save(new Shift("A0"));
			shiftRepository.save(new Shift("55"));
			shiftRepository.save(new Shift("A8"));
			shiftRepository.save(new Shift("常日"));
			shiftRepository.save(new Shift("哺乳"));
			shiftRepository.save(new Shift("加班"));

			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("2222", "Blaire", "C"));
			staffRepository.save(new Staff("3333", "Connie", "C"));
			staffRepository.save(new Staff("4444", "Derek", "C"));
			staffRepository.save(new Staff("5555", "Erick", "C"));
			staffRepository.save(new Staff("6666", "Frank", "C"));
			staffRepository.save(new Staff("7777", "Gina", "C"));
			staffRepository.save(new Staff("8888", "Henry", "B"));
			staffRepository.save(new Staff("9999", "Iris", "B"));
			staffRepository.save(new Staff("9999", "Iris", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));

			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("2222", "Blaire", "C"));
			staffRepository.save(new Staff("3333", "Connie", "C"));
			staffRepository.save(new Staff("2345", "Keren", "B"));
			staffRepository.save(new Staff("5555", "Erick", "C"));
			staffRepository.save(new Staff("6666", "Frank", "C"));
			staffRepository.save(new Staff("5678", "Mike", "B"));
			staffRepository.save(new Staff("5678", "Mike", "B"));
			staffRepository.save(new Staff("8888", "Henry", "B"));
			staffRepository.save(new Staff("9999", "Iris", "B"));
			staffRepository.save(new Staff("9999", "Iris", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));

			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("2222", "Blaire", "C"));
			staffRepository.save(new Staff("3333", "Connie", "C"));
			staffRepository.save(new Staff("3456", "Laren", "B"));
			staffRepository.save(new Staff("4567", "Nancy", "B"));
			staffRepository.save(new Staff("3333", "Connie", "C"));
			staffRepository.save(new Staff("4444", "Derek", "C"));
			staffRepository.save(new Staff("2345", "Keren", "B"));
			staffRepository.save(new Staff("5555", "Erick", "C"));
			staffRepository.save(new Staff("6666", "Frank", "C"));
			staffRepository.save(new Staff("6666", "Frank", "C"));
			staffRepository.save(new Staff("5678", "Mike", "B"));
			staffRepository.save(new Staff("5678", "Mike", "B"));
			staffRepository.save(new Staff("5678", "Mike", "B"));
			staffRepository.save(new Staff("6789", "Oliver", "A"));
			staffRepository.save(new Staff("7891", "Peter", "A"));
			staffRepository.save(new Staff("9999", "Iris", "B"));
			staffRepository.save(new Staff("9999", "Iris", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));

			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("1111", "Aero", "C"));
			staffRepository.save(new Staff("2222", "Blaire", "C"));
			staffRepository.save(new Staff("3333", "Connie", "C"));
			staffRepository.save(new Staff("3456", "Laren", "B"));
			staffRepository.save(new Staff("4567", "Nancy", "B"));
			staffRepository.save(new Staff("3333", "Connie", "C"));
			staffRepository.save(new Staff("4444", "Derek", "C"));
			staffRepository.save(new Staff("2345", "Keren", "B"));
			staffRepository.save(new Staff("5555", "Erick", "C"));
			staffRepository.save(new Staff("6666", "Frank", "C"));
			staffRepository.save(new Staff("6666", "Frank", "C"));
			staffRepository.save(new Staff("5678", "Mike", "B"));
			staffRepository.save(new Staff("5678", "Mike", "B"));
			staffRepository.save(new Staff("5678", "Mike", "B"));
			staffRepository.save(new Staff("6789", "Oliver", "A"));
			staffRepository.save(new Staff("7891", "Peter", "A"));
			staffRepository.save(new Staff("9999", "Iris", "B"));
			staffRepository.save(new Staff("9999", "Iris", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));
			staffRepository.save(new Staff("1234", "Jack", "B"));

			Staff staff = staffRepository.findAll(Sort.by("id")).iterator().next();
			staff.setTimeslot(timeslotRepository.findAll(Sort.by("id")).iterator().next());
			staff.setShift(shiftRepository.findAll(Sort.by("id")).iterator().next());
			staffRepository.save(staff);
		};
	}

	public enum DemoData {
		NONE, SMALL,
	}

}