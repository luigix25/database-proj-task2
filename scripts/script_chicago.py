import json
import datetime
import csv

timestamp_format 	= "%Y-%m-%d %H:%M:%S"
city				= "Chicago"
city_tag			= "CHI"

with open('../datasets/chicago_small.csv') as csvfile:
	reader = csv.DictReader(csvfile)
	for row in reader:
		normalized_row 	= {}
		time 			= {}
		space 			= {}
		rider 			= {}

		time["timestamp_start"] = row["starttime"]
		time["timestamp_end"] 	= row["stoptime"]

		#start_station_id 	= row["start_station_id"][:-2]
		#end_station_id 		= row["end_station_id"][:-2]

		space["station_start"] 	= city_tag +":"+ row["from_station_id"]
		space["station_end"] 	= city_tag +":"+ row["to_station_id"]

		space["latitude_start"] = row["latitude_start"]
		space["latitude_end"] 	= row["latitude_end"]

		space["longitude_start"] = row["longitude_start"]
		space["longitude_end"] 	 = row["longitude_end"]

		rider["gender"]			= 'M' if row["gender"] == 'Male' else 'F'
		rider["type"]			= row["usertype"]

		normalized_row["time"] 	= time
		normalized_row["city"] 	= city
		normalized_row["space"] = space
		normalized_row["rider"] = rider

		print(json.dumps(normalized_row))
