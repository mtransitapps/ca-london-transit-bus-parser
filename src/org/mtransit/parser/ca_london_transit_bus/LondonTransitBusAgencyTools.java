package org.mtransit.parser.ca_london_transit_bus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
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

// http://ltconline.ca/gtfsfeed/google_transit.zip
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

	@Override
	public String getRouteColor(GRoute gRoute) {
		int rsn = Integer.parseInt(gRoute.getRouteShortName());
		switch (rsn) {
		// @formatter:off
		case 1: return "007393";
		case 2: return "006468";
		case 3: return "A78B6B";
		case 4: return "91278F";
		case 5: return "72BF44";
		case 6: return "496E6E";
		case 7: return "0071BC";
		case 8: return "7670B3";
		case 9: return "A47275";
		case 10: return "0066B3";
		case 11: return "F05B72";
		case 12: return "A54686";
		case 13: return "485E88";
		case 14: return "3B8476";
		case 15: return "00AAAD";
		case 16: return "00A650";
		case 17: return "ED1C24";
		case 18: return "D1AC3E";
		case 19: return "009BDF";
		case 20: return "00839B";
		case 21: return "00AEEF";
		case 22: return "2E3092";
		case 23: return "CD8C5D";
		case 24: return "EC008C";
		case 25: return "D2AB67";
		case 26: return "806A50";
		case 27: return "665874";
		case 28: return "0086A6";
		case 29: return "A55560";
		case 30: return "BD1A8D";
		case 31: return "008641";
		case 32: return "F287B7";
		case 33: return "874487";
		case 34: return "0095DA";
		case 35: return "FFC20E";
		case 36: return "D9186B";
		case 37: return "0084BC";
		case 38: return "DA2128";
		case 39: return "B0789A";
		case 40: return "1476C6";
		case 90: return "F5821F";
		case 91: return "F5821F";
		case 92: return "F5821F";
		case 102: return "006468";
		case 104: return "91278F";
		case 106: return "868A5B";
		case 400: return "0FAB4B";
		// @formatter:on
		}
		if (isGoodEnoughAccepted()) {
			return null;
		}
		System.out.printf("\nUnexpected route color %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
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
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private static final String AND = " & ";
	private static final String AT = " @ ";
	private static final String SLASH = " / ";
	private static final String _ = " ";

	private static final String UWO = "UWO";
	private static final String INDUSTRIAL_SHORT = "Ind";

	private static final String ADELAIDE = "Adelaide";
	private static final String ALDERSBROOK = "Aldersbrook";
	private static final String AIRPORT = "Airport";
	private static final String ARGYLE_MALL = "Argyle Mall";
	private static final String BARKER = "Barker";
	private static final String BLACKACRES = "Blackacres";
	private static final String BONAVENTURE = "Bonaventure";
	private static final String BRIARHILL = "Briarhill";
	private static final String BRIXHAM = "Brixham";
	private static final String BYRON_BASELINE = "Byron Baseline";
	private static final String CAPULET = "Capulet";
	private static final String CHELTON = "Chelton";
	private static final String COLONEL_TALBOT = "Colonel Talbot";
	private static final String COMMISSIONERS = "Commissioners";
	private static final String DALHOUSIE = "Dalhousie";
	private static final String DARNLEY = "Darnley";
	private static final String DEVERON = "Deveron";
	private static final String DOWNTOWN = "Downtown";
	private static final String FANSHAWE = "Fanshawe";
	private static final String FANSHAWE_COLLEGE = FANSHAWE + " College";
	private static final String FARRAH = "Farrah";
	private static final String FIRESTONE = "Firestone";
	private static final String GORE = "Gore";
	private static final String GRENFELL = "Grenfell";
	private static final String GRIFFITH = "Griffith";
	private static final String HIGHBURY = "Highbury";
	private static final String HURON = "Huron";
	private static final String HYDE_PARK = "Hyde Pk";
	private static final String INDUSTRIAL_PARK = INDUSTRIAL_SHORT + " Pk";
	private static final String KIPPS = "Kipps";
	private static final String KIPPS_LANE = KIPPS + " Ln";
	private static final String MASONVILLE = "Masonville";
	private static final String MASONVILLE_MALL = MASONVILLE + " Mall";
	private static final String MASONVILLE_PLACE = MASONVILLE + " Pl";
	private static final String MCLEAN = "Mclean";
	private static final String MEADOWGATE = "Meadowgate";
	private static final String NATURAL_SCIENCE = "Natural Science";
	private static final String NORTHRIDGE = "Northridge";
	private static final String OAKVILLE = "Oakville";
	private static final String OUTER = "Outer";
	private static final String OXFORD = "Oxford";
	private static final String PARKWOOD_HOSPITAL = "Parkwood Hosp";
	private static final String PORTSMOUTH = "Portsmouth";
	private static final String POWER_CTR = "Power Ctr";
	private static final String PROUDFOOT = "Proudfoot";
	private static final String REARDON = "Reardon";
	private static final String RIDOUT_AND_GRAND = "Ridout & Grand";
	private static final String RIVERSIDE = "Riverside";
	private static final String SOVEREIGN = "Sovereign";
	private static final String STONEY_CREEK = "Stoney Crk";
	private static final String VICTORIA_HOSPITAL = "Victoria Hosp";
	private static final String UNIV_WESTERN_ONTARIO = UWO; // "Univ Western Ontario";
	private static final String WESTERN_UNIVERSITY = UWO; // "Western University";
	private static final String WESTMOUNT_MALL = "Westmount Mall";
	private static final String WEYMOUTH = "Weymouth";
	private static final String WHARNCLIFFE = "Wharncliffe";
	private static final String WHITE_OAKS_MALL = "White Oaks Mall";
	private static final String WILTON_GROVE = "Wilton Grv";
	private static final String WONDERLAND = "Wonderland";

	private static final String AIRPORT_AND_INDUSTRIAL_AREA = AIRPORT + AND + INDUSTRIAL_SHORT; // + " area";
	private static final String ALDERSBROOK_AT_BLACKACRES = ALDERSBROOK + AT + BLACKACRES;
	private static final String BARKER_AT_HURON = BARKER + AT + HURON;
	private static final String BYRON_BASELINE_AT_GRIFFITH = BYRON_BASELINE + AT + GRIFFITH;
	private static final String CAPULET_AT_OXFORD = CAPULET + AT + OXFORD;
	private static final String CHELTON_AT_REARDON = CHELTON + AT + REARDON;
	private static final String COMMISSIONERS_AT_DEVERON = COMMISSIONERS + AT + DEVERON;
	private static final String DALHOUSIE_AT_BRIXHAM = DALHOUSIE + AT + BRIXHAM;
	private static final String DARNLEY_AT_MEADOWGATE = DARNLEY + AT + MEADOWGATE;
	private static final String FANSHAWE_AT_MCLEAN = FANSHAWE + AT + MCLEAN;
	private static final String FARRAH_AT_PROUDFOOT = FARRAH + AT + PROUDFOOT;
	private static final String FIRESTONE_AT_GORE = FIRESTONE + AT + GORE;
	private static final String GORE_AT_FIRESTONE = GORE + AT + FIRESTONE;
	private static final String GRENFELL_NORTHRIDGE = GRENFELL + SLASH + NORTHRIDGE;
	private static final String GRIFFITH_AT_BYRON_BASELINE = GRIFFITH + AT + BYRON_BASELINE;
	private static final String HURON_AT_HIGHBURY = HURON + AT + HIGHBURY;
	private static final String HYDE_PARK_POWER_CENTRE = HYDE_PARK + _ + POWER_CTR;
	private static final String KIPPS_AT_ADELAIDE = KIPPS + AT + ADELAIDE;
	private static final String KIPPS_LANE_AT_BRIARHILL = KIPPS_LANE + AT + BRIARHILL;
	private static final String OAKVILLE_AT_HURON = OAKVILLE + AT + HURON;
	private static final String OUTER_AT_COLONEL_TALBOT = OUTER + AT + COLONEL_TALBOT;
	private static final String RIVERSIDE_AT_WHARNCLIFFE_ONLY = RIVERSIDE + AT + WHARNCLIFFE; // + " only";
	private static final String WEYMOUTH_AT_PORTSMOUTH = WEYMOUTH + AT + PORTSMOUTH;
	private static final String WILTON_GROVE_INDUSTRIAL_PARK = WILTON_GROVE + _ + INDUSTRIAL_PARK;
	private static final String WONDERLAND_AT_OXFORD = WONDERLAND + AT + OXFORD;
	private static final String WONDERLAND_OXFORD = WONDERLAND + SLASH + OXFORD;
	private static final String WONDERLAND_POWER_CENTRE = WONDERLAND + _ + POWER_CTR;

	private static final int LTC_EASTBOUND = 1;
	private static final int LTC_NORTHBOUND = 2;
	private static final int LTC_SOUTHBOUND = 3;
	private static final int LTC_WESTBOUND = 4;

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (!isGoodEnoughAccepted()) {
			if (false) {
				return; // TODO split trip
			}
			mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
			return;
		}
		if (mRoute.getId() == 1l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(COMMISSIONERS_AT_DEVERON, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(KIPPS_LANE_AT_BRIARHILL, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 2l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(BONAVENTURE, LTC_EASTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WESTERN_UNIVERSITY, LTC_WESTBOUND);
				return;
			}
		} else if (mRoute.getId() == 3l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(GORE_AT_FIRESTONE, LTC_EASTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_WESTBOUND);
				return;
			}
		} else if (mRoute.getId() == 4l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WHITE_OAKS_MALL, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(FANSHAWE_COLLEGE, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 5l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(GRIFFITH_AT_BYRON_BASELINE, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 6l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WESTERN_UNIVERSITY, LTC_NORTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(PARKWOOD_HOSPITAL, LTC_SOUTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 7l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(ARGYLE_MALL, LTC_EASTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_WESTBOUND);
				return;
			}
		} else if (mRoute.getId() == 8l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(BYRON_BASELINE_AT_GRIFFITH, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 9l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(ALDERSBROOK_AT_BLACKACRES, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 10l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(MASONVILLE, LTC_NORTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WHITE_OAKS_MALL, LTC_SOUTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 11l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WESTMOUNT_MALL, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 12l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WONDERLAND_POWER_CENTRE, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 13l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WHITE_OAKS_MALL, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(GRENFELL_NORTHRIDGE, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 14l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(BARKER_AT_HURON, LTC_NORTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WHITE_OAKS_MALL, LTC_SOUTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 15l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WESTMOUNT_MALL, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 16l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(DARNLEY_AT_MEADOWGATE, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(MASONVILLE_MALL, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 17l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(FIRESTONE_AT_GORE, LTC_EASTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString("BYRON_BASELINE_AT_GRIFFITH", LTC_WESTBOUND);
				return;
			}
		} else if (mRoute.getId() == 18l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(RIVERSIDE_AT_WHARNCLIFFE_ONLY, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WESTERN_UNIVERSITY, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 19l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(HYDE_PARK_POWER_CENTRE, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 20l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(FANSHAWE_COLLEGE, LTC_EASTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(CAPULET_AT_OXFORD, LTC_WESTBOUND);
				return;
			}
		} else if (mRoute.getId() == 21l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(OAKVILLE_AT_HURON, LTC_NORTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_SOUTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 22l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(ARGYLE_MALL, LTC_EASTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_WESTBOUND);
				return;
			}
		} else if (mRoute.getId() == 23l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(DALHOUSIE_AT_BRIXHAM, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 24l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WESTMOUNT_MALL, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(CHELTON_AT_REARDON, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 25l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(FANSHAWE_AT_MCLEAN, LTC_NORTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(FANSHAWE_COLLEGE, LTC_SOUTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 26l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WHITE_OAKS_MALL, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 27l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(KIPPS_AT_ADELAIDE, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(FANSHAWE_COLLEGE, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 28l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(OUTER_AT_COLONEL_TALBOT, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WESTMOUNT_MALL, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 29l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(CAPULET, LTC_NORTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WONDERLAND_OXFORD, LTC_SOUTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 30l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WILTON_GROVE_INDUSTRIAL_PARK, LTC_EASTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WHITE_OAKS_MALL, LTC_WESTBOUND);
				return;
			}
		} else if (mRoute.getId() == 31l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(HYDE_PARK_POWER_CENTRE, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(UNIV_WESTERN_ONTARIO, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 32l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(HURON_AT_HIGHBURY, LTC_EASTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WESTERN_UNIVERSITY, LTC_WESTBOUND);
				return;
			}
		} else if (mRoute.getId() == 33l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(FARRAH_AT_PROUDFOOT, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WESTERN_UNIVERSITY, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 34l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(MASONVILLE_MALL, LTC_EASTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WESTERN_UNIVERSITY, LTC_WESTBOUND);
				return;
			}
		} else if (mRoute.getId() == 35l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WEYMOUTH_AT_PORTSMOUTH, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(ARGYLE_MALL, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 36l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(AIRPORT_AND_INDUSTRIAL_AREA, LTC_EASTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(FANSHAWE_COLLEGE, LTC_WESTBOUND);
				return;
			}
		} else if (mRoute.getId() == 37l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(SOVEREIGN, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(ARGYLE_MALL, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 38l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(STONEY_CREEK, LTC_NORTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(MASONVILLE_MALL, LTC_SOUTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 39l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(HYDE_PARK_POWER_CENTRE, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(MASONVILLE_MALL, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 40L) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(NORTHRIDGE, LTC_WESTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(MASONVILLE, LTC_EASTBOUND);
				return;
			}
		} else if (mRoute.getId() == 90l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WHITE_OAKS_MALL, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(MASONVILLE_PLACE, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 91l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(FANSHAWE_COLLEGE, LTC_EASTBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WONDERLAND_AT_OXFORD, LTC_WESTBOUND);
				return;
			}
		} else if (mRoute.getId() == 92l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(MASONVILLE_PLACE, LTC_NORTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(VICTORIA_HOSPITAL, LTC_SOUTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 102l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_SOUTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(NATURAL_SCIENCE, LTC_NORTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 104l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(FANSHAWE_COLLEGE, LTC_NORTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(RIDOUT_AND_GRAND, LTC_SOUTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 106l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(NATURAL_SCIENCE, LTC_NORTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DOWNTOWN, LTC_SOUTHBOUND);
				return;
			}
		} else if (mRoute.getId() == 400l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(FANSHAWE, LTC_NORTHBOUND);
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(ARGYLE_MALL, LTC_SOUTHBOUND);
				return;
			}
		}
		System.out.printf("\n%s: Unexpected trip %s!\n", mRoute.getId(), gTrip);
		System.exit(-1);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (isGoodEnoughAccepted()) {
			return super.mergeHeadsign(mTrip, mTripToMerge);
		}
		System.out.printf("\nUnexpected trips to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern ENDS_WITH_VIA = Pattern.compile("(via.*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern AREA = Pattern.compile("((^|\\W){1}(area)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final Pattern INDUSTRIAL = Pattern.compile("((^|\\W){1}(industrial)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String INDUSTRIAL_REPLACEMENT = "$2" + INDUSTRIAL_SHORT + "$4";

	private static final Pattern ONLY = Pattern.compile("((^|\\W){1}(only)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final Pattern UNIVERSITY_OF_WESTERN_ONTARIO = Pattern.compile("((^|\\W){1}(univ western ontario|western university)(\\W|$){1})",
			Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITY_OF_WESTERN_ONTARIO_REPLACEMENT = "$2" + UWO + "$4";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = AREA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = INDUSTRIAL.matcher(tripHeadsign).replaceAll(INDUSTRIAL_REPLACEMENT);
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

	private static final Pattern HOS = Pattern.compile("((^|\\W){1}(hos)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String HOS_REPLACEMENT = "$2Hospital$4";

	@Override
	public String cleanStopName(String gStopName) {
		if (Utils.isUppercaseOnly(gStopName, true, true)) {
			gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		}
		gStopName = BOUNDS.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = ENDS_WITH_STOP_CODE.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = HOS.matcher(gStopName).replaceAll(HOS_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public int getStopId(GStop gStop) {
		try {
			if (!StringUtils.isEmpty(gStop.getStopCode()) && Utils.isDigitsOnly(gStop.getStopCode())) {
				return Integer.parseInt(gStop.getStopCode()); // use stop code as stop ID
			}
			if (Utils.isDigitsOnly(gStop.getStopId())) {
				return 100000 + Integer.parseInt(gStop.getStopId());
			}
			Matcher matcher = DIGITS.matcher(gStop.getStopId());
			if (matcher.find()) {
				return 100000 + Integer.parseInt(matcher.group());
			}
			System.out.printf("\nUnexpected stop ID for %s!\n", gStop);
			System.exit(-1);
			return -1;
		} catch (Exception e) {
			System.out.printf("\nUnexpected stop ID error for %s!\n", gStop);
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	@Override
	public String getStopCode(GStop gStop) {
		if ("'".equals(gStop.getStopCode())) {
			return null;
		}
		return super.getStopCode(gStop);
	}
}
