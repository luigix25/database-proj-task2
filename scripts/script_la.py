import json
import datetime
import csv
import re

# current_timestamp_format 	= "%m/%d/%Y %H:%M"
current_timestamp_format	= "%Y-%m-%d %H:%M:%S"
standard_timestamp_format 	= "%Y-%m-%d %H:%M:%S"

city				= "Los Angeles"
city_tag			= "LA"

with open('../datasets/la.csv', newline='') as csvfile:
	reader = csv.DictReader(csvfile)
	count = 0
	for row in reader:
		count += 1
		normalized_row 	= {}
		time 			= {}
		space 			= {}
		rider 			= {}

		# normalize year
		year = 2015 + count % 4
		time_start 	= datetime.datetime.strptime(re.sub('201\\d', str(year), row["start_time"]), current_timestamp_format)
		time_end 	= datetime.datetime.strptime(re.sub('201\\d', str(year), row["end_time"]), current_timestamp_format)

		time["year"] = year
		time["timestamp_start"] = datetime.datetime.strftime(time_start,standard_timestamp_format)
		time["timestamp_end"] 	= datetime.datetime.strftime(time_end,standard_timestamp_format)

		start_station_id 	= row["start_station"]
		end_station_id 		= row["end_station"]

		space["station_start"] 	= city_tag +":"+ start_station_id
		space["station_end"] 	= city_tag +":"+ end_station_id

		space["latitude_start"] = row["start_lat"]
		space["latitude_end"] 	= row["end_lat"]

		space["longitude_start"] = row["start_lon"]
		space["longitude_end"] 	 = row["end_lon"]

		rider["type"]			= row["passholder_type"]

		normalized_row["time"] 	= time
		normalized_row["city"] 	= city
		normalized_row["space"] = space
		normalized_row["rider"]	= rider
		print(json.dumps(normalized_row))