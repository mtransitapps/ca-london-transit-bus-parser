package org.mtransit.parser.ca_london_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// http://www.londontransit.ca/open-data/
// http://www.londontransit.ca/gtfsfeed/google_transit.zip
public class LondonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-london-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new LondonTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating London Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating London Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return String.valueOf(Integer.parseInt(gRoute.getRouteShortName())); // remove leading 0
		}
		return super.getRouteShortName(gRoute);
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "009F60"; // GREEN (from web site CSS)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private static final String SLASH = " / ";

	private static final String UWO = "UWO";
	private static final String INDUSTRIAL_SHORT = "Ind";
	//
	private static final String MASONVILLE = "Masonville";
	private static final String HOSPITAL_SHORT = "Hosp";

	private static final int LTC_EASTBOUND = 1;
	private static final int LTC_NORTHBOUND = 2;
	private static final int LTC_SOUTHBOUND = 3;
	private static final int LTC_WESTBOUND = 4;

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (mRoute.getId() == 1L) {
			if (gTrip.getDirectionId() == 1) { // Kipps Lane - NORTH
				if (Arrays.asList( //
						"1B King edward & Thompson Only", //
						"King edward & Thompson Only", //
						"1A Pond Mills & Thompson Only", //
						"Pond Mills & Thompson Only", //
						"1A Kipps Lane via Wellington & Dundas", //
						"1B Kipps Lane via Wellington & Dundas", //
						"Kipps Lane via Wellington & Dundas" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Pond Mills - SOUTH
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"Wellington & Dundas Only", //
						"1B King Edward via Wellington & Dundas", //
						"2B King Edward via Wellington & Dundas", // WTF!
						"King Edward via Wellington & Dundas", //
						"1A Pond Mills Road via Wellington & Dundas", //
						"Pond Mills Road via Wellington & Dundas", //
						"1A Pond Mills via Wellington & Dundas", //
						"Pond Mills via Wellington & Dundas" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 2L) {
			if (gTrip.getDirectionId() == 0) { // Argyle Mall / Bonaventure - EAST
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"2 to Dundas and Highbury Only", //
						"2 to Dundas & Highbury Only", //
						"Dundas & Highbury Only", //
						"2A Trafalgar Heights Only", //
						"2A to Trafalgar Heights", //
						"Trafalgar Heights Only", //
						"2B Bonaventure via Dundas", //
						"Bonaventure via Dundas", //
						"2A Argyle Mall via Hale & Trafalgar", //
						"Argyle Mall via Hale & Trafalgar" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Natural Science - WEST
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"2B Argyle Mall Only", //
						"Argyle Mall Only", //
						"2 to Natural Science via Dundas", //
						"2A", //
						"2A Natural Science via Dundas", //
						"2B Natural Science via Dundas", //
						"Natural Science via Dundas" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 3L) {
			if (gTrip.getDirectionId() == 1) { // Argyle Mall - EAST
				if (Arrays.asList( //
						"3B Argyle Mall via Hamilton", //
						"Argyle Mall via Hamilton", //
						"3A Argyle Mall via Fairmont", //
						"Argyle Mall via Fairmont" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Downtown - WEST
				if (Arrays.asList( //
						"3A Highbury & Hamilton Only", //
						"3B Highbury & Hamilton Only", //
						"Highbury & Hamilton Only", //
						"3B Downtown via Hamilton", //
						"3A Downtown via Hamilton", //
						"Downtown via Hamilton" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 4L) {
			if (gTrip.getDirectionId() == 1) { // Fanshawe College - NORTH
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"4B Southdale & Ernest Only", //
						"4 Southdale & Ernest Only", //
						"Southdale & Ernest Only", //
						"4A Fanshawe College via Downtown", //
						"4B Fanshawe College via Downtown", //
						"4 to Fanshawe College", //
						"4 Fanshawe College via Downtown", //
						"Fanshawe College via Downtown" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // White Oaks Mall - SOUTH
				if (Arrays.asList( //
						"4A White Oaks Mall via Downtown", //
						"4B White Oaks Mall via Downtown", //
						"White Oaks Mall via Downtown" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 5L) {
			if (gTrip.getDirectionId() == 1) { // Downtown - EAST
				if (Arrays.asList( //
						"5 Springbank & Wonderland Only", //
						"Springbank & Wonderland Only", //
						"5B Downtown via Gardenwood",//
						"Downtown via Gardenwood", //
						"5A Downtown via Springbank", //
						"5B Downtown via Springbank", //
						"Downtown via Springbank" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Byron - WEST
				if (Arrays.asList( //
						"5B Byron via Gardenwood", //
						"Byron via Gardenwood", //
						"5A Byron via Springbank", //
						"5 Byron via Springbank", //
						"Byron via Springbank" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 6L) {
			if (gTrip.getDirectionId() == 0) { // Natural Science - NORTH
				if (Arrays.asList( //
						"Baseline & Fairview Only", //
						"Natural Science via Richmond" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Parkwood - SOUTH
				if (Arrays.asList( //
						"Richmond & Dundas Only", //
						"Parkwood Hospital via Richmond", //
						"Parkwood Hosptial via Richmond", //
						"Parkwood via Richmond" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 7L) {
			if (gTrip.getDirectionId() == 0) { // Argyle Mall - EAST
				if (Arrays.asList( //
						"Argyle Mall", //
						"Argyle Mall via York" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Downtown - WEST
				if (Arrays.asList( //
						"Brydges & Highbury Only", //
						"Downtown via York" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 9L) {
			if (gTrip.getDirectionId() == 1) { // Downtown - EAST
				if (Arrays.asList( //
						"9C Sarnia & Wonderland Only", //
						"Sarnia & Wonderland Only", //
						"9A Downtown via Sarnia", //
						"9B Downtown via Sarnia", //
						"Downtown via Sarnia", //
						"9C Natural Science via Sarnia", //
						"Natural Science via Sarnia" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Whitehills - WEST
				if (Arrays.asList( //
						"9A Whitehills via Limberlost", //
						"9C Whitehills via Limberlost", //
						"Whitehills via Limberlost", //
						"9B Whitehills via Aldersbrook", //
						"Whitehills via Aldersbrook" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 10L) {
			if (gTrip.getDirectionId() == 0) { // Masonville - NORTH
				if (Arrays.asList( //
						"Natural Science via Wonderland", //
						"Masonvile via Wonderland" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // White Oaks Mall - SOUTH
				if (Arrays.asList( //
						"White Oaks Mall via Wonderland" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 11L) {
			if (gTrip.getDirectionId() == 1) { // Downtown - EAST
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"Downtown Via Wharncliffe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Westmount Mall - WEST
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"Westmount Mall via Wharncliffe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 12L) {
			if (gTrip.getDirectionId() == 1) { // Downtown - NORTH
				if (Arrays.asList( //
						"Commissioners & Wharncliffe Only", //
						"Downtown via Wharncliffe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Wharncliffe & Wonderland - SOUTH
				if (Arrays.asList( //
						"Wharncliffe & Wonderland Only", //
						"Wharncliffe & Wonderland via Whancliffe", //
						"Wharncliffe & Wonderland via Wharncliffe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 13L) {
			if (gTrip.getDirectionId() == 1) { // Masonville Mall - NORTH
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"13 to Masonville Mall", //
						"13 Masonville Mall Only", //
						"Masonville Mall Only", //
						"13A Masonville Mall via Westminster Park", //
						"Masonville Mall via Westminster Park", //
						"13 Masonville Mall via Wellington & Richmond", //
						"Masonville Mall via Wellington & Richmond" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // White Oaks Mall - SOUTH
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"13 to White Oaks Mall", //
						"13A White Oaks Mall via Westminster Park", //
						"White Oaks Mall via Westminster Park", //
						"13 White Oaks Mall via Richmond & Wellington", //
						"White Oaks Mall via Richmond & Wellington" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 14L) {
			if (gTrip.getDirectionId() == 0) { // Huron & Barker - NORTH
				if (Arrays.asList( //
						"Highbury & Brydges Only", //
						"Huron & Barker via Highbury" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // White Oaks Mall - SOUTH
				if (Arrays.asList( //
						"Highbury & Brydges Only", //
						"White Oaks Mall via Highbury" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 15L) {
			if (gTrip.getDirectionId() == 1) { // Downtown - NORTH
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"15A to Dalhousie and Brixham Only", //
						"15A to Dalhousie and Brixham", //
						"15 B To Cranbrook", //
						"15B to Cranbrook and Commissioners only", //
						"15A- Downtown via Old South", //
						"15A Downtown via Old South", //
						"Downtown via Old South", //
						"15B Downtown via Cranbrook", //
						"Downtown via Cranbrook" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Westmount Mall - SOUTH
				if (Arrays.asList( //
						"15A Westmount Mall via Old South", //
						"15 B Westmount Mall via Old South", //
						"15B Westmount Mall via Old South", //
						"Westmount Mall via Old South" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 16L) {
			if (gTrip.getDirectionId() == 1) { // Masonville Mall - NORTH
				if (Arrays.asList( //
						"16 Commissioners & Highbury Only", //
						"Commissioners & Highbury Only", //
						"16A Commissioners at Deveron Only", //
						"Commissioners at Deveron Only", //
						"16 Summerside via Pond Mills", //
						"Summerside via Pond Mills", //
						"16 Masonville Mall via Adelaide", //
						"16B Masvonille Mall via Adelaide", //
						"16B Masonville via Adelaide", //
						"Masonville via Adelaide", //
						"Masvonille Mall via Adelaide", // WTF!
						"16A Masonville Mall via Adelaide", //
						"16B Masonville Mall via Adelaide", //
						"Masonville Mall via Adelaide" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Summerside - SOUTH
				if (Arrays.asList( //
						"16 Adelaide & King Only", //
						"Adelaide & King Only", //
						"16A Pond Mills via Adelaide", //
						"Pond Mills via Adelaide", //
						"16 Pond Mills & Summerside via Adelaide", //
						"Pond Mills & Summerside via Adelaide", //
						"16B Summerside via Adelaide", //
						"Summerside via Adelaide", //
						"16B Summerside", //
						"Summerside" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 17L) {
			if (gTrip.getDirectionId() == 0) { // Argyle Mall - EAST
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"17A Boler & Commissioners Only", //
						"Boler & Commissioners Only", //
						"17A Argyle Mall via Oxford", //
						"17B Argyle Mall via Oxford", //
						"Argyle Mall via Oxford" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Byron / Riverbend - WEST
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"17B Riverbend via Oxford", //
						"Riverbend via Oxford", //
						"17A Byron via Oxford", //
						"17B Byron via Oxford", //
						"Byron via Oxford" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 19L) {
			if (gTrip.getDirectionId() == 1) { // Downtown - EAST
				if (Arrays.asList( //
						"Wellington & Dundas Only", //
						"Hyde Park Power Centre via Hyde Park", //
						"Downtown via Hyde Park" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Hyde Pk Power Ctr - WEST
				if (Arrays.asList( //
						"Hyde Park Power Centre via Hyde Park" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 20L) {
			if (gTrip.getDirectionId() == 0) { // Fanshawe College - EAST
				if (Arrays.asList( //
						"Beaverbrook & Wonderland Only", //
						"Richmond & Dundas Only", //
						"Downtown Only", //
						"Fanshawe College via Downtown" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Beaverbrook - WEST
				if (Arrays.asList( //
						"Beaverbrook via Downtown", //
						"Beaverbrook via Downtow" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 21L) {
			if (gTrip.getDirectionId() == 0) { // Huron Hts - NORTH
				if (Arrays.asList( //
						"Huron Heights via Cheapside" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Downtown - SOUTH
				if (Arrays.asList( //
						"Huron & Highbury Only", //
						"Downtown via Cheapside" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 24L) {
			if (gTrip.getDirectionId() == 1) { // Victoria Hosp - EAST
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"Westmount Mall Only", //
						"Victoria Hospital via Baseline" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Talbot Vlg - WEST
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"Westmount Mall Only", //
						"Talbot Village via Baseline" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 25L) {
			if (gTrip.getDirectionId() == 0) { // Masonville Mall - NORTH
				if (Arrays.asList( //
						"Masonville Mall via Grenfell" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Fanshawe College - SOUTH
				if (Arrays.asList( //
						"Fanshawe College via Grenfell" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 26L) {
			if (gTrip.getDirectionId() == 1) { // Downtown - NORTH
				if (Arrays.asList( //
						"Jalna & Bradley Only", //
						"Downtown via Wharncliffe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // White Oaks Mall - SOUTH
				if (Arrays.asList( //
						"White Oaks Mall via Wharncliffe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 27L) {
			if (gTrip.getDirectionId() == 1) { // Fanshawe College - EAST
				if (Arrays.asList( //
						"Fanshawe College via Huron" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Kipps Lane - WEST
				if (Arrays.asList( //
						"Kipps Lane via Huron" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 28L) {
			if (gTrip.getDirectionId() == 1) { // Westmount Mall - NORTH
				if (Arrays.asList( //
						"Wonderland & Wharncliffe only", //
						"Westmount Mall via Wonderland" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Lambeth - SOUTH
				if (Arrays.asList( //
						"Lambeth via Wonderland" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 29L) {
			if (gTrip.getDirectionId() == 0) { // Natural Science - NORTH
				if (Arrays.asList( //
						"Natural Science via Sarnia" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Capulet Lane - SOUTH
				if (Arrays.asList( //
						"Natural Sciences via Sarnia", //
						"Capulet Lane via Sarnia" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 30L) {
			if (gTrip.getDirectionId() == 0) { // Cheese Factory Road - EAST
				if (Arrays.asList( //
						"Cheese Factory Road via Newbold" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // White Oaks Mall - WEST
				if (Arrays.asList( //
						"White Oaks Mall via Wilton Grove" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 31L) {
			if (gTrip.getDirectionId() == 1) { // Alumni Hall - EAST
				if (Arrays.asList( //
						"Alumni Hall via Limberlost" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Hyde Park Power Centre - WEST
				if (Arrays.asList( //
						"Hyde Park Power Centre via Limberlost" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 32L) {
			if (gTrip.getDirectionId() == 0) { // Huron & Highbury - EAST
				if (Arrays.asList( //
						"Huron & Highbury via Windermere" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Alumni Hall - WEST
				if (Arrays.asList( //
						"Adelaide & Huron Only", //
						"Alumni Hall via Windermere" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 33L) {
			if (gTrip.getDirectionId() == 1) { // Alumni Hall - EAST
				if (Arrays.asList( //
						"Proudfoot & Oxford Only", //
						"Alumni Hall via Platts Lane" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Proudfoot - WEST
				if (Arrays.asList( //
						"Proudfoot via Platts Lane" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 34L) {
			if (gTrip.getDirectionId() == 0) { // Masonville Mall - EAST
				if (Arrays.asList( //
						"Masonville Mall via Ambleside" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Alumni Hall - WEST
				if (Arrays.asList( //
						"Alumni Hall via Western Road" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 35L) {
			if (gTrip.getDirectionId() == 1) { // Argyle Mall - NORTH
				if (Arrays.asList( //
						"Argyle Mall via Marconi" // <>
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Trafalgar Heights - SOUTH
				if (Arrays.asList( //
						"Argyle Mall via Marconi", // <>
						"Trafalgar Heights", //
						"Trafalgar Heights via Marconi" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 36L) {
			if (gTrip.getDirectionId() == 0) { // London Airport - EAST
				if (Arrays.asList( //
						"London Airport via Oxford" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Fanshawe College - WEST
				if (Arrays.asList( //
						"Fanshawe College Only", //
						"Fanshawe College via Oxford" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 37L) {
			if (gTrip.getDirectionId() == 1) { // Argyle Mall - NORTH
				if (Arrays.asList( //
						"Sovereign & Trafalgar Only", //
						"Argyle Mall via Sovereign" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Neptune Crescent - SOUTH
				if (Arrays.asList( //
						"Neptune Crescent via Sovereign" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 38L) {
			if (gTrip.getDirectionId() == 0) { // Stoney Creek - NORTH
				if (Arrays.asList( //
						"Stoney Creek via Sunningdale" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Masonville Mall - SOUTH
				if (Arrays.asList( //
						StringUtils.EMPTY, //
						"Masonville Mall via Sunningdale" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 39L) {
			if (gTrip.getDirectionId() == 1) { // Masonville Mall - EAST
				if (Arrays.asList( //
						"Masonville Mall via Fanshawe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Hyde Park Power Centre - WEST
				if (Arrays.asList( //
						"Hyde Park Power Centre via Fanshawe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 40L) {
			if (gTrip.getDirectionId() == 0) { // Northridge - EAST
				if (Arrays.asList( //
						"Northridge & Grenfell via Fanshawe", //
						"Northridge via Fanshawe Pk Rd", //
						"Northridge via Fanshawe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Masonville - WEST
				if (Arrays.asList( //
						"Fanshawe & Adelaide Only", //
						"Masonville via Northridge & Grenfell", //
						"Masonville via Fanshawe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 90L) {
			if (gTrip.getDirectionId() == 1) { // Masonville Mall - NORTH
				if (Arrays.asList( //
						"Express to Masonville Mall" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // White Oaks Mall - SOUTH
				if (Arrays.asList( //
						"Express to White Oaks Mall" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 91L) {
			if (gTrip.getDirectionId() == 0) { // Fanshawe College - EAST
				if (Arrays.asList( //
						"Express to Fanshawe College" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_EASTBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Oxford & Wonderland - WEST
				if (Arrays.asList( //
						"Express to Oxford and Wonderland", //
						"Express to Oxford & Wonderland" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_WESTBOUND);
					return;
				}
			}
		} else if (mTrip.getRouteId() == 92L) {
			if (gTrip.getDirectionId() == 0) { // Masonville Mall - NORTH
				if (Arrays.asList( //
						"Express to Masonville Mall" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Victoria Hosp - SOUTH
				if (Arrays.asList( //
						"Express to Victoria Hosptial", //
						"Express to Victoria Hospital" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 102L) {
			if (gTrip.getDirectionId() == 1) { // Natural Science - NORTH
				if (Arrays.asList( //
						"Natural Science via Western/Wharncliffe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 0) { // Downtown - SOUTH
				if (Arrays.asList( //
						"Richmond at King Only via Western/Wharncliffe", //
						"King & Richmond Only via Western/Wharncliffe", //
						"Downtown via Western/Wharncliffe" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 104L) {
			if (gTrip.getDirectionId() == 0) { // Fanshawe College - NORTH
				if (Arrays.asList( //
						"Fanshawe College via Oxford" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Ridout & Grand - SOUTH
				if (Arrays.asList( //
						"Richmond & Dundas Only", //
						"Ridout & Grand via Oxford" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		} else if (mRoute.getId() == 106L) {
			if (gTrip.getDirectionId() == 0) { // Natural Science - NORTH
				if (Arrays.asList( //
						"Natural Science via Richmond" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_NORTHBOUND);
					return;
				}
			} else if (gTrip.getDirectionId() == 1) { // Downtown - SOUTH
				if (Arrays.asList( //
						"Downtown via Richmond" //
				).contains(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), LTC_SOUTHBOUND);
					return;
				}
			}
		}
		System.out.printf("\n%s: Unexpected trip %s!\n", mRoute.getId(), gTrip);
		System.exit(-1);
	}

	private static final String A = "A";
	private static final String A_ = "A ";
	private static final String B_ = "B ";
	private static final String C_ = "C ";

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (MTrip.mergeEmpty(mTrip, mTripToMerge)) {
			return true;
		}
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 1L) {
			if (Arrays.asList( //
					"Wellington & Dundas", //
					B_ + "King Edward", //
					"King Edward", //
					A_ + "Pond Mills Rd", //
					"Pond Mills Rd", //
					A_ + "Pond Mills", //
					"Pond Mills" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Pond Mills", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					B_ + "King Edward & Thompson", //
					"King Edward & Thompson", //
					A_ + "Pond Mills & Thompson", //
					"Pond Mills & Thompson", //
					A_ + "Kipps Ln", //
					B_ + "Kipps Ln", //
					"Kipps Ln" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Kipps Ln", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 2L) {
			if (Arrays.asList( //
					"Dundas & Highbury", //
					A_ + "Trafalgar Hts", //
					"Trafalgar Hts", //
					A_ + "Argyle Mall", // <>
					"Argyle Mall", // <>
					B_ + "Bonaventure", //
					"Bonaventure", //
					"Argyle Mall" + SLASH + "Bonaventure" // ++
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Argyle Mall" + SLASH + "Bonaventure", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Argyle Mall", // <>
					B_ + "Argyle Mall", //
					A, //
					A_ + "Natural Science", //
					B_ + "Natural Science", //
					"Natural Science" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Natural Science", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 3L) {
			if (Arrays.asList( //
					A_ + "Highbury & Hamilton", //
					B_ + "Highbury & Hamilton", //
					"Highbury & Hamilton", //
					A_ + "Downtown", //
					B_ + "Downtown", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					A_ + "Argyle Mall", //
					B_ + "Argyle Mall", //
					"Argyle Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Argyle Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 4L) {
			if (Arrays.asList( //
					B_ + "Southdale & Ernest", //
					"Southdale & Ernest", //
					A_ + "Fanshawe College", //
					B_ + "Fanshawe College", //
					"Fanshawe College" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Fanshawe College", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					A_ + "White Oaks Mall", //
					B_ + "White Oaks Mall", //
					"White Oaks Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("White Oaks Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 5L) {
			if (Arrays.asList( //
					"Springbank & Wonderland", //
					A_ + "Downtown", //
					B_ + "Downtown", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					A_ + "Byron", //
					B_ + "Byron", //
					"Byron" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Byron", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 6L) {
			if (Arrays.asList( //
					"Richmond & Dundas", //
					"Parkwood", //
					"Parkwood Hosp" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Parkwood Hosp", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Baseline & Fairview", //
					"Natural Science" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Natural Science", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 7L) {
			if (Arrays.asList( //
					"Brydges & Highbury", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 9L) {
			if (Arrays.asList( //
					C_ + "Sarnia & Wonderland", //
					"Sarnia & Wonderland", //
					C_ + "Natural Science", //
					"Natural Science", //
					A_ + "Downtown", //
					B_ + "Downtown", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					A_ + "Whitehills", //
					B_ + "Whitehills", //
					C_ + "Whitehills", //
					"Whitehills" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Whitehills", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 10L) {
			if (Arrays.asList( //
					"Natural Science", //
					"Masonville" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 12L) {
			if (Arrays.asList( //
					"Wharncliffe & Wonderland", //
					"Commissioners & Wharncliffe", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 13L) {
			if (Arrays.asList( //
					A_ + "White Oaks Mall", //
					"White Oaks Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("White Oaks Mall", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					A_ + "Masonville Mall", //
					"Masonville Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 14L) {
			if (Arrays.asList( //
					"Highbury & Brydges", // <>
					"Huron & Barker" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Huron & Barker", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Highbury & Brydges", // <>
					"White Oaks Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("White Oaks Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 15L) {
			if (Arrays.asList( //
					B_ + "Cranbrook & Commissioners", //
					A_ + "Dalhousie & Brixham", //
					B_ + "Cranbrook", //
					A_ + "Downtown", //
					A + "- Downtown", //
					B_ + "Downtown", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					A_ + "Westmount Mall", //
					B_ + "Westmount Mall", //
					"Westmount Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Westmount Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 16L) {
			if (Arrays.asList( //
					"Adelaide & King", //
					A_ + "Pond Mills", //
					"Pond Mills", //
					"Pond Mills & Summerside", //
					B_ + "Summerside", // <>
					"Summerside" // <>
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Summerside", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Summerside", // <>
					"Commissioners & Highbury", //
					A_ + "Commissioners @ Deveron", //
					"Commissioners @ Deveron", //
					B_ + "Masonville", //
					"Masonville", //
					A_ + "Masonville Mall", //
					B_ + "Masonville Mall", //
					"Masonville Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 17L) {
			if (Arrays.asList( //
					A_ + "Boler & Commissioners", //
					"Boler & Commissioners", //
					A_ + "Argyle Mall", //
					B_ + "Argyle Mall", //
					"Argyle Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Argyle Mall", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					B_ + "Riverbend", //
					"Riverbend", //
					A_ + "Byron",//
					B_ + "Byron",//
					"Byron", //
					"Byron" + SLASH + "Riverbend" // ++
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Byron" + SLASH + "Riverbend", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 19L) {
			if (Arrays.asList( //
					"Hyde Pk Power Ctr", // <>
					"Wellington & Dundas", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 20L) {
			if (Arrays.asList( //
					"Beaverbrook & Wonderland", //
					"Downtown", //
					"Richmond & Dundas", //
					"Fanshawe College" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Fanshawe College", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 21L) {
			if (Arrays.asList( //
					"Huron & Highbury", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 24L) {
			if (Arrays.asList( //
					"Westmount Mall", // <>
					"Talbot Vlg" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Talbot Vlg", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Westmount Mall", // <>
					"Victoria Hosp" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Victoria Hosp", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 26L) {
			if (Arrays.asList( //
					"Jalna & Bradley", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 28L) {
			if (Arrays.asList( //
					"Wonderland & Wharncliffe", //
					"Westmount Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Westmount Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 29L) {
			if (Arrays.asList( //
					"Natural Sciences", // <>
					"Capulet Ln" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Capulet Ln", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 32L) {
			if (Arrays.asList( //
					"Adelaide & Huron", //
					"Alumni Hall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Alumni Hall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 33L) {
			if (Arrays.asList( //
					"Proudfoot & Oxford", //
					"Alumni Hall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Alumni Hall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 35L) {
			if (Arrays.asList( //
					"Argyle Mall", // <>
					"Trafalgar Hts" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Trafalgar Hts", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 37L) {
			if (Arrays.asList( //
					"Sovereign & Trafalgar", //
					"Argyle Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Argyle Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 40L) {
			if (Arrays.asList( //
					"Fanshawe & Adelaide", //
					"Masonville" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Masonville", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Northridge & Grenfell", //
					"Northridge" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Northridge", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 102L) {
			if (Arrays.asList( //
					"Richmond @ King", //
					"King & Richmond", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 104L) {
			if (Arrays.asList( //
					"Richmond & Dundas", //
					"Ridout & Grand" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Ridout & Grand", mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern ENDS_WITH_VIA = Pattern.compile("(via.*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern AREA = Pattern.compile("((^|\\W){1}(area)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final Pattern INDUSTRIAL = Pattern.compile("((^|\\W){1}(industrial)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String INDUSTRIAL_REPLACEMENT = "$2" + INDUSTRIAL_SHORT + "$4";

	private static final Pattern HOSPITAL_ = Pattern.compile("((^|\\W){1}(hosptial|hos)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String HOSPITAL_REPLACEMENT = "$2" + HOSPITAL_SHORT + "$4";

	private static final Pattern MASONVILLE_ = Pattern.compile("((^|\\W){1}(masonvile|masvonille|masonvillel|masonville)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String MASONVILLE_REPLACEMENT = "$2" + MASONVILLE + "$4";

	private static final Pattern ONLY = Pattern.compile("((^|\\W){1}(only)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final Pattern UNIVERSITY_OF_WESTERN_ONTARIO = Pattern.compile("((^|\\W){1}(univ western ontario|western university)(\\W|$){1})",
			Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITY_OF_WESTERN_ONTARIO_REPLACEMENT = "$2" + UWO + "$4";

	private static final Pattern STARTS_WITH_EXPRESS_TO = Pattern.compile("(^express to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_TO = Pattern.compile("(([A-Z]{1}[\\s]+)?(.*)([\\s]?to )(.*))", Pattern.CASE_INSENSITIVE);
	private static final String STARTS_WITH_TO_REPLACEMENT = "$2$5";

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+[\\s]?)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY); // 1st
		tripHeadsign = STARTS_WITH_TO.matcher(tripHeadsign).replaceAll(STARTS_WITH_TO_REPLACEMENT); // 2nd
		tripHeadsign = STARTS_WITH_EXPRESS_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = AREA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = INDUSTRIAL.matcher(tripHeadsign).replaceAll(INDUSTRIAL_REPLACEMENT);
		tripHeadsign = HOSPITAL_.matcher(tripHeadsign).replaceAll(HOSPITAL_REPLACEMENT);
		tripHeadsign = MASONVILLE_.matcher(tripHeadsign).replaceAll(MASONVILLE_REPLACEMENT);
		tripHeadsign = ONLY.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = UNIVERSITY_OF_WESTERN_ONTARIO.matcher(tripHeadsign).replaceAll(UNIVERSITY_OF_WESTERN_ONTARIO_REPLACEMENT);
		tripHeadsign = ENDS_WITH_VIA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern BOUNDS = Pattern.compile("(eb|wb|nb|sb|fs|ns)", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_STOP_CODE = Pattern.compile("( \\- #[\\d]*[\\w]*[\\']*$)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		if (Utils.isUppercaseOnly(gStopName, true, true)) {
			gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		}
		gStopName = BOUNDS.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = ENDS_WITH_STOP_CODE.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = HOSPITAL_.matcher(gStopName).replaceAll(HOSPITAL_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		return super.getStopId(gStop); // used by real-time API
	}

	@Override
	public String getStopCode(GStop gStop) {
		if ("'".equals(gStop.getStopCode())) {
			return null;
		}
		return super.getStopCode(gStop);
	}
}
