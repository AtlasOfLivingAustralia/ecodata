load('uuid.js');

var ramsar = ["Apsley Marshes", "Ashmore Reef National Nature Reserve", "Banrock Station Wetland Complex", "Barmah Forest", "Becher Point Wetlands", "Blue Lake", "Bool And Hacks Lagoons", "Bowling Green Bay", "Cobourg Peninsula", "Coongie Lakes", "Corner Inlet", "Currawinya Lakes (Currawinya National Park)", "East Coast Cape Barren Island Lagoons", "Edithvale-Seaford Wetlands", "Eighty-Mile Beach", "Fivebough And Tuckerbil Swamps", "Flood Plain Lower Ringarooma River", "Forrestdale And Thomsons Lakes", "Ginini Flats Wetland Complex", "Gippsland Lakes", "Great Sandy Strait (Including Great Sandy Strait, Tin Can Bay And Tin Can Inlet)", "Gunbower Forest", "Gwydir Wetlands: Gingham And Lower Gwydir (Big Leather) Watercourses", "Hattah-Kulkyne Lakes", "Hosnies Spring", "Hunter Estuary Wetlands", "Interlaken (Lake Crescent)", "Jocks Lagoon", "Kakadu National Park", "Kerang Wetlands", "Lake Albacutya", "Lake Gore", "Lake Pinaroo (Fort Grey Basin)", "Lake Warden System", "Lakes Argyle And Kununurra", "Lavinia", "Little Llangothlin Nature Reserve", "Little Waterhouse Lake", "Logan Lagoon", "Moreton Bay", "Moulting Lagoon", "Muir-Byenup System", "Myall Lakes", "Narran Lake Nature Reserve", "Nsw Central Murray State Forests", "Ord River Floodplain", "Paroo River Wetlands", "Peel-Yalgorup System", "Piccaninnie Ponds Karst Wetlands", "Pitt Water-Orielton Lagoon", "Port Phillip Bay (Western Shoreline) And Bellarine Peninsula", "Pulu Keeling National Park", "Riverland", "Roebuck Bay", "Shoalwater And Corio Bays Area (Shoalwater Bay Training Area, In Part - Corio Bay)", "The Coorong, And Lakes Alexandrina And Albert Wetland", "The Dales", "The Macquarie Marshes", "Toolibin Lake (Also Known As Lake Toolibin)", "Towra Point Nature Reserve", "Vasse-Wonnerup System", "Western District Lakes", "Western Port"]
var mus = ["Central Tablelands", "Central West", "Greater Sydney", "Hunter", "Murray", "North Coast", "North Coast - Lord Howe Island", "North West NSW", "Northern Tablelands", "Riverina", "South East NSW", "Western", "Corangamite", "East Gippsland", "Glenelg Hopkins", "Goulburn Broken", "Mallee", "North Central", "North East", "Port Phillip and Western Port", "West Gippsland", "Wimmera", "Burnett Mary", "Cape York", "Condamine", "Co-operative Management Area", "Desert Channels", "Fitzroy", "Burdekin", "Northern Gulf", "Maranoa Balonne and Border Rivers", "Mackay Whitsunday", "South East Queensland", "South West Queensland", "Southern Gulf", "Wet Tropics", "Torres Strait", "Adelaide and Mount Lofty Ranges", "Alinytjara Wilurara", "Eyre Peninsula", "Kangaroo Island", "Northern and Yorke", "South Australian Arid Lands", "South Australian Murray Darling Basin", "South East", "Northern Agricultural Region", "Peel-Harvey Region", "Swan Region", "Rangelands Region", "South Coast Region", "South West Region", "Avon River Basin", "North West NRM Region", "North NRM Region", "South NRM Region", "South NRM Region - Macquarie Islands", "Northern Territory", "ACT"];
var ramsarByMu = {
    "South NRM Region": ["Apsley Marshes", "Interlaken (Lake Crescent)", "Moulting Lagoon", "Pitt Water-Orielton Lagoon"],
    "South Australian Murray Darling Basin": ["Banrock Station Wetland Complex", "Riverland", "The Coorong, And Lakes Alexandrina And Albert Wetland"],
    "Murray": ["Barmah Forest", "Blue Lake", "Gunbower Forest", "Nsw Central Murray State Forests"],
    "Goulburn Broken": ["Barmah Forest", "Nsw Central Murray State Forests"],
    "Swan Region": ["Becher Point Wetlands", "Forrestdale And Thomsons Lakes"],
    "South East NSW": ["Blue Lake"],
    "South East": ["Bool And Hacks Lagoons", "Piccaninnie Ponds Karst Wetlands", "The Coorong, And Lakes Alexandrina And Albert Wetland"],
    "Burdekin": ["Bowling Green Bay"],
    "Northern Territory": ["Cobourg Peninsula", "Kakadu National Park", "Lakes Argyle And Kununurra"],
    "Desert Channels": ["Coongie Lakes"],
    "South Australian Arid Lands": ["Coongie Lakes"],
    "West Gippsland": ["Corner Inlet", "Gippsland Lakes", "Western Port"],
    "Western": ["Currawinya Lakes (Currawinya National Park)", "Lake Pinaroo (Fort Grey Basin)", "Narran Lake Nature Reserve", "Paroo River Wetlands", "Riverland"],
    "South West Queensland": ["Currawinya Lakes (Currawinya National Park)"],
    "North NRM Region": ["East Coast Cape Barren Island Lagoons", "Flood Plain Lower Ringarooma River", "Jocks Lagoon", "Little Waterhouse Lake", "Logan Lagoon"],
    "Port Phillip and Western Port": ["Edithvale-Seaford Wetlands", "Port Phillip Bay (Western Shoreline) And Bellarine Peninsula", "Western Port"],
    "Rangelands Region": ["Eighty-Mile Beach", "Lakes Argyle And Kununurra", "Ord River Floodplain", "Roebuck Bay"],
    "Riverina": ["Fivebough And Tuckerbil Swamps", "Ginini Flats Wetland Complex"],
    "Peel-Harvey Region": ["Forrestdale And Thomsons Lakes", "Peel-Yalgorup System"],
    "ACT": ["Ginini Flats Wetland Complex"],
    "East Gippsland": ["Gippsland Lakes"],
    "Burnett Mary": ["Great Sandy Strait (Including Great Sandy Strait, Tin Can Bay And Tin Can Inlet)"],
    "South East Queensland": ["Great Sandy Strait (Including Great Sandy Strait, Tin Can Bay And Tin Can Inlet)", "Moreton Bay"],
    "North Central": ["Gunbower Forest", "Kerang Wetlands", "Nsw Central Murray State Forests"],
    "North West NSW": ["Gwydir Wetlands: Gingham And Lower Gwydir (Big Leather) Watercourses", "Narran Lake Nature Reserve"],
    "Mallee": ["Hattah-Kulkyne Lakes", "Riverland"],
    "Hunter": ["Hunter Estuary Wetlands", "Myall Lakes"],
    "Wimmera": ["Lake Albacutya"],
    "South Coast Region": ["Lake Gore", "Lake Warden System", "Muir-Byenup System"],
    "North West NRM Region": ["Lavinia"],
    "Northern Tablelands": ["Little Llangothlin Nature Reserve"],
    "South West Region": ["Muir-Byenup System", "Toolibin Lake (Also Known As Lake Toolibin)", "Vasse-Wonnerup System"],
    "Glenelg Hopkins": ["Piccaninnie Ponds Karst Wetlands", "Western District Lakes"],
    "Corangamite": ["Port Phillip Bay (Western Shoreline) And Bellarine Peninsula", "Western District Lakes"],
    "Fitzroy": ["Shoalwater And Corio Bays Area (Shoalwater Bay Training Area, In Part - Corio Bay)"],
    "Central West": ["The Macquarie Marshes"],
    "Greater Sydney": ["Towra Point Nature Reserve"]
};

var teCommunities = ["Alpine Sphagnum Bogs and Associated Fens", "Aquatic Root Mat Community 1 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 2 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 3 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 4 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community in Caves of the Swan Coastal Plain", "Arnhem Plateau Sandstone Shrubland Complex", "Assemblages of plants and invertebrate animals of tumulus (organic mound) springs of the Swan Coastal Plain", "Banksia Woodlands of the Swan Coastal Plain ecological community", "Blue Gum High Forest of the Sydney Basin Bioregion", "Brigalow (Acacia harpophylla dominant and co-dominant)", "Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Castlereagh Scribbly Gum and Agnes Banks Woodlands of the Sydney Basin Bioregion", "Central Hunter Valley eucalypt forest and woodland", "Clay Pans of the Swan Coastal Plain", "Coastal Upland Swamps in the Sydney Basin Bioregion", "Cooks River/Castlereagh Ironbark Forest of the Sydney Basin Bioregion", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Corymbia calophylla - Kingia australis woodlands on heavy soils of the Swan Coastal Plain", "Corymbia calophylla - Xanthorrhoea preissii woodlands and shrublands of the Swan Coastal Plain", "Cumberland Plain Shale Woodlands and Shale-Gravel Transition Forest", "Eastern Stirling Range Montane Heath and Thicket", "Eastern Suburbs Banksia Scrub of the Sydney Region", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Eucalyptus ovata - Callitris oblonga Forest", "Eyre Peninsula Blue Gum (Eucalyptus petiolaris) Woodland", "Giant Kelp Marine Forests of South East Australia", "Gippsland Red Gum (Eucalyptus tereticornis subsp. mediana) Grassy Woodland and Associated Native Grassland", "Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Hunter Valley Weeping Myall (Acacia pendula) Woodland", "Illawarra and south coast lowland forest and woodland ecological community", "Iron-grass Natural Temperate Grassland of South Australia", "Kangaroo Island Narrow-leaved Mallee (Eucalyptus cneorifolia) Woodland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Grassy Woodland in the South East Corner Bioregion", "Lowland Native Grasslands of Tasmania", "Lowland Rainforest of Subtropical Australia", "Mabi Forest (Complex Notophyll Vine Forest 5b)", "Monsoon vine thickets on the coastal sand dunes of Dampier Peninsula", "Natural Damp Grassland of the Victorian Coastal Plains", "Natural Grasslands of the Murray Valley Plains", "Natural Grasslands of the Queensland Central Highlands and northern Fitzroy Basin", "Natural Temperate Grassland of the South Eastern Highlands", "Natural Temperate Grassland of the Victorian Volcanic Plain", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands", "Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Perched Wetlands of the Wheatbelt region with extensive stands of living sheoak and paperbark across the lake floor (Toolibin Lake)", "Posidonia australis seagrass meadows of the Manning-Hawkesbury ecoregion", "Proteaceae Dominated Kwongkan Shrublands of the Southeast Coastal Floristic Province of Western Australia", "Scott River Ironstone Association", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Sedgelands in Holocene dune swales of the southern Swan Coastal Plain", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Shale Sandstone Transition Forest of the Sydney Basin Bioregion", "Shrublands and Woodlands of the eastern Swan Coastal Plain", "Shrublands and Woodlands on Muchea Limestone of the Swan Coastal Plain", "Shrublands and Woodlands on Perth to Gingin ironstone (Perth to Gingin ironstone association) of the Swan Coastal Plain", "Shrublands on southern Swan Coastal Plain ironstones", "Silurian Limestone Pomaderris Shrubland of the South East Corner and Australian Alps Bioregions", "Southern Highlands Shale Forest and Woodland in the Sydney Basin Bioregion", "Subtropical and Temperate Coastal Saltmarsh", "Swamp Tea-tree (Melaleuca irbyana) Forest of South-east Queensland", "Swamps of the Fleurieu Peninsula", "Temperate Highland Peat Swamps on Sandstone", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Thrombolite (microbial) community of coastal freshwater lakes of the Swan Coastal Plain (Lake Richmond)", "Thrombolite (microbialite) Community of a Coastal Brackish Lake (Lake Clifton)", "Turpentine-Ironbark Forest of the Sydney Basin Bioregion", "Upland Basalt Eucalypt Forests of the Sydney Basin Bioregion", "Upland Wetlands of the New England Tablelands (New England Tableland Bioregion) and the Monaro Plateau (South Eastern Highlands Bioregion)", "Weeping Myall Woodlands", "Western Sydney Dry Rainforest and Moist Woodland on Shale", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"];
var teCommunitesByMu = {
    "Murray": ["Alpine Sphagnum Bogs and Associated Fens", "Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Natural Temperate Grassland of the South Eastern Highlands", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Riverina": ["Alpine Sphagnum Bogs and Associated Fens", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Natural Temperate Grassland of the South Eastern Highlands", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South East NSW": ["Alpine Sphagnum Bogs and Associated Fens", "Coastal Upland Swamps in the Sydney Basin Bioregion", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Illawarra and south coast lowland forest and woodland ecological community", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Grassy Woodland in the South East Corner Bioregion", "Natural Temperate Grassland of the South Eastern Highlands", "Shale Sandstone Transition Forest of the Sydney Basin Bioregion", "Southern Highlands Shale Forest and Woodland in the Sydney Basin Bioregion", "Subtropical and Temperate Coastal Saltmarsh", "Turpentine-Ironbark Forest of the Sydney Basin Bioregion", "Upland Basalt Eucalypt Forests of the Sydney Basin Bioregion", "Upland Wetlands of the New England Tablelands (New England Tableland Bioregion) and the Monaro Plateau (South Eastern Highlands Bioregion)", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "East Gippsland": ["Alpine Sphagnum Bogs and Associated Fens", "Gippsland Red Gum (Eucalyptus tereticornis subsp. mediana) Grassy Woodland and Associated Native Grassland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Natural Temperate Grassland of the South Eastern Highlands", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Silurian Limestone Pomaderris Shrubland of the South East Corner and Australian Alps Bioregions", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Goulburn Broken": ["Alpine Sphagnum Bogs and Associated Fens", "Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "North East": ["Alpine Sphagnum Bogs and Associated Fens", "Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Temperate Grassland of the South Eastern Highlands", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Port Phillip and Western Port": ["Alpine Sphagnum Bogs and Associated Fens", "Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Natural Damp Grassland of the Victorian Coastal Plains", "Natural Temperate Grassland of the Victorian Volcanic Plain", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "West Gippsland": ["Alpine Sphagnum Bogs and Associated Fens", "Gippsland Red Gum (Eucalyptus tereticornis subsp. mediana) Grassy Woodland and Associated Native Grassland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Natural Damp Grassland of the Victorian Coastal Plains", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "North West NRM Region": ["Alpine Sphagnum Bogs and Associated Fens", "Lowland Native Grasslands of Tasmania", "Subtropical and Temperate Coastal Saltmarsh"],
    "North NRM Region": ["Alpine Sphagnum Bogs and Associated Fens", "Eucalyptus ovata - Callitris oblonga Forest", "Giant Kelp Marine Forests of South East Australia", "Lowland Native Grasslands of Tasmania", "Subtropical and Temperate Coastal Saltmarsh"],
    "South NRM Region": ["Alpine Sphagnum Bogs and Associated Fens", "Eucalyptus ovata - Callitris oblonga Forest", "Giant Kelp Marine Forests of South East Australia", "Lowland Native Grasslands of Tasmania", "Subtropical and Temperate Coastal Saltmarsh"],
    "ACT": ["Alpine Sphagnum Bogs and Associated Fens", "Natural Temperate Grassland of the South Eastern Highlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South West Region": ["Aquatic Root Mat Community 1 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 2 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 3 in Caves of the Leeuwin Naturaliste Ridge", "Aquatic Root Mat Community 4 in Caves of the Leeuwin Naturaliste Ridge", "Banksia Woodlands of the Swan Coastal Plain ecological community", "Clay Pans of the Swan Coastal Plain", "Corymbia calophylla - Xanthorrhoea preissii woodlands and shrublands of the Swan Coastal Plain", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Perched Wetlands of the Wheatbelt region with extensive stands of living sheoak and paperbark across the lake floor (Toolibin Lake)", "Scott River Ironstone Association", "Shrublands on southern Swan Coastal Plain ironstones", "Subtropical and Temperate Coastal Saltmarsh"],
    "Swan Region": ["Aquatic Root Mat Community in Caves of the Swan Coastal Plain", "Assemblages of plants and invertebrate animals of tumulus (organic mound) springs of the Swan Coastal Plain", "Banksia Woodlands of the Swan Coastal Plain ecological community", "Clay Pans of the Swan Coastal Plain", "Corymbia calophylla - Kingia australis woodlands on heavy soils of the Swan Coastal Plain", "Corymbia calophylla - Xanthorrhoea preissii woodlands and shrublands of the Swan Coastal Plain", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Sedgelands in Holocene dune swales of the southern Swan Coastal Plain", "Shrublands and Woodlands of the eastern Swan Coastal Plain", "Shrublands and Woodlands on Muchea Limestone of the Swan Coastal Plain", "Shrublands and Woodlands on Perth to Gingin ironstone (Perth to Gingin ironstone association) of the Swan Coastal Plain", "Subtropical and Temperate Coastal Saltmarsh", "Thrombolite (microbial) community of coastal freshwater lakes of the Swan Coastal Plain (Lake Richmond)"],
    "Northern Territory": ["Arnhem Plateau Sandstone Shrubland Complex"],
    "Northern Agricultural Region": ["Banksia Woodlands of the Swan Coastal Plain ecological community", "Clay Pans of the Swan Coastal Plain", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Shrublands and Woodlands on Muchea Limestone of the Swan Coastal Plain", "Shrublands and Woodlands on Perth to Gingin ironstone (Perth to Gingin ironstone association) of the Swan Coastal Plain", "Subtropical and Temperate Coastal Saltmarsh"],
    "Peel-Harvey Region": ["Banksia Woodlands of the Swan Coastal Plain ecological community", "Clay Pans of the Swan Coastal Plain", "Corymbia calophylla - Kingia australis woodlands on heavy soils of the Swan Coastal Plain", "Corymbia calophylla - Xanthorrhoea preissii woodlands and shrublands of the Swan Coastal Plain", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Sedgelands in Holocene dune swales of the southern Swan Coastal Plain", "Subtropical and Temperate Coastal Saltmarsh", "Thrombolite (microbialite) Community of a Coastal Brackish Lake (Lake Clifton)"],
    "Greater Sydney": ["Blue Gum High Forest of the Sydney Basin Bioregion", "Castlereagh Scribbly Gum and Agnes Banks Woodlands of the Sydney Basin Bioregion", "Coastal Upland Swamps in the Sydney Basin Bioregion", "Cooks River/Castlereagh Ironbark Forest of the Sydney Basin Bioregion", "Cumberland Plain Shale Woodlands and Shale-Gravel Transition Forest", "Eastern Suburbs Banksia Scrub of the Sydney Region", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Posidonia australis seagrass meadows of the Manning-Hawkesbury ecoregion", "Shale Sandstone Transition Forest of the Sydney Basin Bioregion", "Subtropical and Temperate Coastal Saltmarsh", "Temperate Highland Peat Swamps on Sandstone", "Turpentine-Ironbark Forest of the Sydney Basin Bioregion", "Upland Basalt Eucalypt Forests of the Sydney Basin Bioregion", "Western Sydney Dry Rainforest and Moist Woodland on Shale", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "North West NSW": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Northern Tablelands": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Upland Wetlands of the New England Tablelands (New England Tableland Bioregion) and the Monaro Plateau (South Eastern Highlands Bioregion)", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Western": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands"],
    "Burnett Mary": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Subtropical and Temperate Coastal Saltmarsh", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Condamine": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Desert Channels": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands"],
    "Fitzroy": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Rainforest of Subtropical Australia", "Natural Grasslands of the Queensland Central Highlands and northern Fitzroy Basin", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Subtropical and Temperate Coastal Saltmarsh", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Burdekin": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Natural Grasslands of the Queensland Central Highlands and northern Fitzroy Basin", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands"],
    "Maranoa Balonne and Border Rivers": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Natural Grasslands of the Queensland Central Highlands and northern Fitzroy Basin", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South East Queensland": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "Subtropical and Temperate Coastal Saltmarsh", "Swamp Tea-tree (Melaleuca irbyana) Forest of South-east Queensland", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South West Queensland": ["Brigalow (Acacia harpophylla dominant and co-dominant)", "Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Natural Grasslands of the Queensland Central Highlands and northern Fitzroy Basin", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin", "Weeping Myall Woodlands"],
    "Cape York": ["Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Mabi Forest (Complex Notophyll Vine Forest 5b)"],
    "Northern Gulf": ["Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin"],
    "Mackay Whitsunday": ["Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Semi-evergreen vine thickets of the Brigalow Belt (North and South) and Nandewar Bioregions"],
    "Wet Tropics": ["Broad leaf tea-tree (Melaleuca viridiflora) woodlands in high rainfall coastal north Queensland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Mabi Forest (Complex Notophyll Vine Forest 5b)"],
    "Mallee": ["Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "North Central": ["Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Natural Temperate Grassland of the Victorian Volcanic Plain", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Wimmera": ["Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South East": ["Buloke Woodlands of the Riverina and Murray-Darling Depression Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Grasslands of the Murray Valley Plains", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Hunter": ["Central Hunter Valley eucalypt forest and woodland", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Hunter Valley Weeping Myall (Acacia pendula) Woodland", "Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Rainforest of Subtropical Australia", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "Posidonia australis seagrass meadows of the Manning-Hawkesbury ecoregion", "Subtropical and Temperate Coastal Saltmarsh", "Upland Basalt Eucalypt Forests of the Sydney Basin Bioregion", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South Coast Region": ["Clay Pans of the Swan Coastal Plain", "Eastern Stirling Range Montane Heath and Thicket", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Proteaceae Dominated Kwongkan Shrublands of the Southeast Coastal Floristic Province of Western Australia", "Subtropical and Temperate Coastal Saltmarsh"],
    "Avon River Basin": ["Clay Pans of the Swan Coastal Plain", "Eucalypt Woodlands of the Western Australian Wheatbelt", "Proteaceae Dominated Kwongkan Shrublands of the Southeast Coastal Floristic Province of Western Australia"],
    "Central West": ["Coolibah - Black Box Woodlands of the Darling Riverine Plains and the Brigalow Belt South Bioregions", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Temperate Grassland of the South Eastern Highlands", "Natural grasslands on basalt and fine-textured alluvial plains of northern New South Wales and southern Queensland", "Weeping Myall Woodlands", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Rangelands Region": ["Eucalypt Woodlands of the Western Australian Wheatbelt", "Monsoon vine thickets on the coastal sand dunes of Dampier Peninsula", "Proteaceae Dominated Kwongkan Shrublands of the Southeast Coastal Floristic Province of Western Australia", "Subtropical and Temperate Coastal Saltmarsh"],
    "Eyre Peninsula": ["Eyre Peninsula Blue Gum (Eucalyptus petiolaris) Woodland", "Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Subtropical and Temperate Coastal Saltmarsh"],
    "Corangamite": ["Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Natural Damp Grassland of the Victorian Coastal Plains", "Natural Temperate Grassland of the Victorian Volcanic Plain", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Glenelg Hopkins": ["Grassy Eucalypt Woodland of the Victorian Volcanic Plain", "Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Temperate Grassland of the Victorian Volcanic Plain", "Seasonal Herbaceous Wetlands (Freshwater) of the Temperate Lowland Plains", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Central Tablelands": ["Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Natural Temperate Grassland of the South Eastern Highlands", "Temperate Highland Peat Swamps on Sandstone", "Upland Basalt Eucalypt Forests of the Sydney Basin Bioregion", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "Adelaide and Mount Lofty Ranges": ["Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Iron-grass Natural Temperate Grassland of South Australia", "Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Subtropical and Temperate Coastal Saltmarsh", "Swamps of the Fleurieu Peninsula"],
    "Northern and Yorke": ["Grey Box (Eucalyptus microcarpa) Grassy Woodlands and Derived Native Grasslands of South-eastern Australia", "Iron-grass Natural Temperate Grassland of South Australia", "Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Subtropical and Temperate Coastal Saltmarsh"],
    "South Australian Murray Darling Basin": ["Iron-grass Natural Temperate Grassland of South Australia", "Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Subtropical and Temperate Coastal Saltmarsh", "Swamps of the Fleurieu Peninsula"],
    "Kangaroo Island": ["Kangaroo Island Narrow-leaved Mallee (Eucalyptus cneorifolia) Woodland", "Subtropical and Temperate Coastal Saltmarsh"],
    "North Coast": ["Littoral Rainforest and Coastal Vine Thickets of Eastern Australia", "Lowland Rainforest of Subtropical Australia", "New England Peppermint (Eucalyptus nova-anglica) Grassy Woodlands", "Subtropical and Temperate Coastal Saltmarsh", "White Box-Yellow Box-Blakely's Red Gum Grassy Woodland and Derived Native Grassland"],
    "South Australian Arid Lands": ["Peppermint Box (Eucalyptus odorata) Grassy Woodland of South Australia", "Subtropical and Temperate Coastal Saltmarsh", "The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin"],
    "Southern Gulf": ["The community of native species dependent on natural discharge of groundwater from the Great Artesian Basin"]
};

var worldHeritageSites = ["Fraser Island", "Gondwana Rainforests of Australia", "Great Barrier Reef", "Kakadu National Park", "Lord Howe Island Group", "Purnululu National Park", "Shark Bay, Western Australia", "Tasmanian Wilderness", "The Greater Blue Mountains Area", "The Ningaloo Coast", "Uluru - Kata Tjuta National Park", "Wet Tropics of Queensland", "Willandra Lakes Region"];
var worldHeritageSitesByMu = {
    "Burnett Mary": ["Fraser Island", "Great Barrier Reef"],
    "Hunter": ["Gondwana Rainforests of Australia", "The Greater Blue Mountains Area"],
    "North Coast": ["Gondwana Rainforests of Australia"],
    "Northern Tablelands": ["Gondwana Rainforests of Australia"],
    "Condamine": ["Gondwana Rainforests of Australia"],
    "South East Queensland": ["Gondwana Rainforests of Australia"],
    "Cape York": ["Great Barrier Reef", "Wet Tropics of Queensland"],
    "Fitzroy": ["Great Barrier Reef"],
    "Burdekin": ["Great Barrier Reef", "Wet Tropics of Queensland"],
    "Mackay Whitsunday": ["Great Barrier Reef"],
    "Wet Tropics": ["Great Barrier Reef", "Wet Tropics of Queensland"],
    "Torres Strait": ["Great Barrier Reef"],
    "Northern Territory": ["Kakadu National Park", "Uluru - Kata Tjuta National Park"],
    "North Coast - Lord Howe Island": ["Lord Howe Island Group"],
    "Rangelands Region": ["Purnululu National Park", "Shark Bay, Western Australia", "The Ningaloo Coast"],
    "Northern Agricultural Region": ["Shark Bay, Western Australia"],
    "North West NRM Region": ["Tasmanian Wilderness"],
    "North NRM Region": ["Tasmanian Wilderness"],
    "South NRM Region": ["Tasmanian Wilderness"],
    "Central Tablelands": ["The Greater Blue Mountains Area"],
    "Greater Sydney": ["The Greater Blue Mountains Area"],
    "South East NSW": ["The Greater Blue Mountains Area"],
    "Co-operative Management Area": ["Wet Tropics of Queensland"],
    "Northern Gulf": ["Wet Tropics of Queensland"],
    "Western": ["Willandra Lakes Region"]
};

var soilPriorities = ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"];
var soilPrioritiesByMU = {
    "Central Tablelands": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Central West": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Greater Sydney": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Hunter": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Murray": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North Coast": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North West NSW": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Northern Tablelands": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Riverina": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South East NSW": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Western": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Corangamite": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "East Gippsland": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Glenelg Hopkins": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Goulburn Broken": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Mallee": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North Central": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North East": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Port Phillip and Western Port": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "West Gippsland": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Wimmera": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Burnett Mary": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Cape York": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Condamine": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Co-operative Management Area": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Desert Channels": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Fitzroy": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Burdekin": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Northern Gulf": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Maranoa Balonne and Border Rivers": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Mackay Whitsunday": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South East Queensland": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South West Queensland": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Southern Gulf": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Wet Tropics": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Adelaide and Mount Lofty Ranges": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Alinytjara Wilurara": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Eyre Peninsula": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Kangaroo Island": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Northern and Yorke": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South Australian Arid Lands": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South Australian Murray Darling Basin": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South East": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Northern Agricultural Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Peel-Harvey Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Swan Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Rangelands Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South Coast Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South West Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Avon River Basin": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North West NRM Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "North NRM Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "South NRM Region": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "Northern Territory": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"],
    "ACT": ["Soil acidification", "Soil Carbon priority", "Hillslope erosion priority", "Wind erosion priority"]
};


var tssSpecies = ["Acacia purpureopetala", "Acacia whibleyana", "Amytornis woodwardi", "Anthochaera phrygia", "Banksia cuneata", "Banksia vincentia", "Bettongia gaimardi", "Bettongia penicillata", "Botaurus poiciloptilus", "Brachychiton sp. Ormeau (L.H.Bird AQ435851)", "Burramys parvus", "Calyptorhynchus banksii graptogyne", "Casuarius casuarius johnsonii", "Conilurus penicillatus", "Cyanoramphus cookii", "Dasyornis brachypterus", "Dasyurus geoffroii", "Dasyurus viverrinus", "Drakaea elastica", "Epacris stuartii", "Epthianura crocea tunneyi", "Eucalyptus crenulata", "Eucalyptus morrisbyi", "Eucalyptus recurva", "Fregata andrewsi", "Grevillea caleyi", "Grevillea calliantha", "Gymnobelideus leadbeateri", "Hibiscus brennanii", "Homoranthus darwinioides", "Isoodon auratus auratus", "Isoodon auratus barrowensis", "Lagorchestes hirsutus Central Australian subspecies", "Lathamus discolor", "Leipoa ocellata", "Lepidorrhachis mooreana", "Lichenostomus melanops cassidix", "Livistona mariae subsp. mariae", "Macadamia jansenii", "Macrotis lagotis", "Myrmecobius fasciatus", "Myrmecodia beccarii", "Neophema chrysogaster", "Ninox novaeseelandiae undulata", "Notomys aquilo", "Numenius madagascariensis", "Oberonia attenuata", "Olearia pannosa subsp. pannosa", "Pedionomus torquatus", "Perameles gunnii Victorian subspecies", "Perameles gunnii gunnii", "Petaurus gracilis", "Petrogale lateralis MacDonnell Ranges race", "Petrogale lateralis West Kimberley race", "Petrogale lateralis hackettii", "Petrogale lateralis lateralis", "Pezoporus flaviventris", "Pezoporus occidentalis", "Pimelea spinescens subsp. spinescens", "Potorous tridactylus gilbertii", "Prasophyllum murfetii", "Psephotus chrysopterygius", "Pseudocheirus occidentalis", "Pteropus natalis", "Ptilotus fasciculatus", "Rutidosis leptorrhynchoides", "Sclerolaena napiformis", "Sminthopsis aitkeni", "Stipiturus mallee", "Swainsona recta", "Syzygium paniculatum", "Tetratheca gunnii", "Thelymitra cyanapicata", "Thinornis rubricollis rubricollis", "Verticordia spicata subsp. squamosa", "Zyzomys pedunculatus"];
var tssSpeciesByMu = {
    "Northern Gulf": ["Acacia purpureopetala", "Casuarius casuarius johnsonii", "Myrmecodia beccarii", "Numenius madagascariensis", "Psephotus chrysopterygius"],
    "Wet Tropics": ["Acacia purpureopetala", "Casuarius casuarius johnsonii", "Myrmecodia beccarii", "Numenius madagascariensis", "Oberonia attenuata", "Petaurus gracilis"],
    "Eyre Peninsula": ["Acacia whibleyana", "Bettongia penicillata", "Leipoa ocellata", "Macrotis lagotis", "Numenius madagascariensis", "Olearia pannosa subsp. pannosa", "Pedionomus torquatus", "Thinornis rubricollis rubricollis"],
    "Northern Territory": ["Amytornis woodwardi", "Conilurus penicillatus", "Epthianura crocea tunneyi", "Hibiscus brennanii", "Isoodon auratus auratus", "Lagorchestes hirsutus Central Australian subspecies", "Leipoa ocellata", "Livistona mariae subsp. mariae", "Macrotis lagotis", "Notomys aquilo", "Numenius madagascariensis", "Petrogale lateralis MacDonnell Ranges race", "Zyzomys pedunculatus"],
    "Central Tablelands": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Homoranthus darwinioides", "Lathamus discolor", "Leipoa ocellata", "Swainsona recta"],
    "Central West": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Homoranthus darwinioides", "Lathamus discolor", "Leipoa ocellata", "Numenius madagascariensis", "Swainsona recta"],
    "Greater Sydney": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Grevillea caleyi", "Homoranthus darwinioides", "Lathamus discolor", "Numenius madagascariensis", "Syzygium paniculatum"],
    "Hunter": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Homoranthus darwinioides", "Lathamus discolor", "Leipoa ocellata", "Numenius madagascariensis", "Syzygium paniculatum"],
    "Murray": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Burramys parvus", "Lathamus discolor", "Leipoa ocellata", "Pedionomus torquatus", "Pimelea spinescens subsp. spinescens", "Sclerolaena napiformis"],
    "North Coast": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Dasyornis brachypterus", "Lathamus discolor", "Numenius madagascariensis"],
    "North West NSW": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Lathamus discolor", "Leipoa ocellata"],
    "Northern Tablelands": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Dasyornis brachypterus", "Lathamus discolor"],
    "Riverina": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Burramys parvus", "Lathamus discolor", "Leipoa ocellata", "Pedionomus torquatus", "Sclerolaena napiformis"],
    "South East NSW": ["Anthochaera phrygia", "Banksia vincentia", "Bettongia gaimardi", "Botaurus poiciloptilus", "Burramys parvus", "Dasyornis brachypterus", "Eucalyptus recurva", "Lathamus discolor", "Leipoa ocellata", "Numenius madagascariensis", "Rutidosis leptorrhynchoides", "Swainsona recta", "Syzygium paniculatum", "Thinornis rubricollis rubricollis"],
    "Corangamite": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Lathamus discolor", "Neophema chrysogaster", "Numenius madagascariensis", "Pedionomus torquatus", "Pimelea spinescens subsp. spinescens", "Rutidosis leptorrhynchoides", "Thinornis rubricollis rubricollis"],
    "East Gippsland": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Burramys parvus", "Dasyornis brachypterus", "Lathamus discolor", "Numenius madagascariensis", "Thinornis rubricollis rubricollis"],
    "Goulburn Broken": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Burramys parvus", "Eucalyptus crenulata", "Gymnobelideus leadbeateri", "Lathamus discolor", "Pedionomus torquatus", "Pimelea spinescens subsp. spinescens", "Sclerolaena napiformis", "Swainsona recta"],
    "North Central": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Lathamus discolor", "Leipoa ocellata", "Numenius madagascariensis", "Pedionomus torquatus", "Pimelea spinescens subsp. spinescens", "Sclerolaena napiformis"],
    "North East": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Burramys parvus", "Lathamus discolor", "Pedionomus torquatus", "Sclerolaena napiformis", "Swainsona recta"],
    "Port Phillip and Western Port": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Eucalyptus crenulata", "Gymnobelideus leadbeateri", "Lathamus discolor", "Lichenostomus melanops cassidix", "Neophema chrysogaster", "Numenius madagascariensis", "Pedionomus torquatus", "Perameles gunnii Victorian subspecies", "Pimelea spinescens subsp. spinescens", "Rutidosis leptorrhynchoides", "Thinornis rubricollis rubricollis"],
    "West Gippsland": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Gymnobelideus leadbeateri", "Lathamus discolor", "Neophema chrysogaster", "Numenius madagascariensis", "Thinornis rubricollis rubricollis"],
    "Wimmera": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Calyptorhynchus banksii graptogyne", "Lathamus discolor", "Leipoa ocellata", "Numenius madagascariensis", "Pedionomus torquatus", "Pimelea spinescens subsp. spinescens", "Rutidosis leptorrhynchoides", "Sclerolaena napiformis"],
    "Burnett Mary": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Dasyornis brachypterus", "Lathamus discolor", "Macadamia jansenii", "Numenius madagascariensis"],
    "Condamine": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Dasyornis brachypterus", "Lathamus discolor"],
    "Maranoa Balonne and Border Rivers": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Lathamus discolor"],
    "South East Queensland": ["Anthochaera phrygia", "Botaurus poiciloptilus", "Brachychiton sp. Ormeau (L.H.Bird AQ435851)", "Dasyornis brachypterus", "Lathamus discolor", "Numenius madagascariensis"],
    "ACT": ["Anthochaera phrygia", "Bettongia gaimardi", "Botaurus poiciloptilus", "Lathamus discolor", "Rutidosis leptorrhynchoides", "Swainsona recta"],
    "Peel-Harvey Region": ["Banksia cuneata", "Bettongia penicillata", "Botaurus poiciloptilus", "Dasyurus geoffroii", "Drakaea elastica", "Leipoa ocellata", "Macrotis lagotis", "Myrmecobius fasciatus", "Numenius madagascariensis", "Pseudocheirus occidentalis"],
    "Avon River Basin": ["Banksia cuneata", "Bettongia penicillata", "Dasyurus geoffroii", "Leipoa ocellata", "Myrmecobius fasciatus", "Petrogale lateralis lateralis", "Ptilotus fasciculatus"],
    "North West NRM Region": ["Bettongia gaimardi", "Botaurus poiciloptilus", "Dasyurus viverrinus", "Lathamus discolor", "Neophema chrysogaster", "Numenius madagascariensis", "Perameles gunnii gunnii", "Tetratheca gunnii", "Thinornis rubricollis rubricollis"],
    "North NRM Region": ["Bettongia gaimardi", "Botaurus poiciloptilus", "Dasyurus viverrinus", "Lathamus discolor", "Numenius madagascariensis", "Perameles gunnii gunnii", "Tetratheca gunnii", "Thinornis rubricollis rubricollis"],
    "South NRM Region": ["Bettongia gaimardi", "Botaurus poiciloptilus", "Dasyurus viverrinus", "Epacris stuartii", "Eucalyptus morrisbyi", "Lathamus discolor", "Neophema chrysogaster", "Numenius madagascariensis", "Perameles gunnii gunnii", "Thinornis rubricollis rubricollis"],
    "Western": ["Bettongia penicillata", "Botaurus poiciloptilus", "Leipoa ocellata", "Macrotis lagotis", "Myrmecobius fasciatus", "Pedionomus torquatus"],
    "Northern Agricultural Region": ["Bettongia penicillata", "Dasyurus geoffroii", "Drakaea elastica", "Grevillea calliantha", "Leipoa ocellata", "Numenius madagascariensis", "Petrogale lateralis lateralis", "Ptilotus fasciculatus", "Verticordia spicata subsp. squamosa"],
    "Swan Region": ["Bettongia penicillata", "Botaurus poiciloptilus", "Dasyurus geoffroii", "Drakaea elastica", "Leipoa ocellata", "Numenius madagascariensis", "Petrogale lateralis lateralis", "Pseudocheirus occidentalis"],
    "Rangelands Region": ["Bettongia penicillata", "Conilurus penicillatus", "Dasyurus geoffroii", "Isoodon auratus auratus", "Isoodon auratus barrowensis", "Lagorchestes hirsutus Central Australian subspecies", "Leipoa ocellata", "Macrotis lagotis", "Myrmecobius fasciatus", "Numenius madagascariensis", "Petrogale lateralis MacDonnell Ranges race", "Petrogale lateralis West Kimberley race", "Petrogale lateralis lateralis", "Pezoporus occidentalis"],
    "South Coast Region": ["Bettongia penicillata", "Botaurus poiciloptilus", "Dasyurus geoffroii", "Leipoa ocellata", "Myrmecobius fasciatus", "Numenius madagascariensis", "Petrogale lateralis hackettii", "Petrogale lateralis lateralis", "Pezoporus flaviventris", "Potorous tridactylus gilbertii", "Pseudocheirus occidentalis"],
    "South West Region": ["Bettongia penicillata", "Botaurus poiciloptilus", "Dasyurus geoffroii", "Drakaea elastica", "Leipoa ocellata", "Myrmecobius fasciatus", "Numenius madagascariensis", "Pseudocheirus occidentalis"],
    "Glenelg Hopkins": ["Botaurus poiciloptilus", "Calyptorhynchus banksii graptogyne", "Lathamus discolor", "Leipoa ocellata", "Neophema chrysogaster", "Numenius madagascariensis", "Pedionomus torquatus", "Perameles gunnii Victorian subspecies", "Pimelea spinescens subsp. spinescens", "Rutidosis leptorrhynchoides", "Thinornis rubricollis rubricollis"],
    "Mallee": ["Botaurus poiciloptilus", "Lathamus discolor", "Leipoa ocellata", "Numenius madagascariensis", "Pedionomus torquatus", "Stipiturus mallee"],
    "Fitzroy": ["Botaurus poiciloptilus", "Macadamia jansenii", "Numenius madagascariensis"],
    "South West Queensland": ["Botaurus poiciloptilus", "Pedionomus torquatus"],
    "Adelaide and Mount Lofty Ranges": ["Botaurus poiciloptilus", "Leipoa ocellata", "Neophema chrysogaster", "Numenius madagascariensis", "Olearia pannosa subsp. pannosa", "Prasophyllum murfetii", "Thelymitra cyanapicata", "Thinornis rubricollis rubricollis"],
    "Kangaroo Island": ["Botaurus poiciloptilus", "Numenius madagascariensis", "Olearia pannosa subsp. pannosa", "Sminthopsis aitkeni", "Thinornis rubricollis rubricollis"],
    "South Australian Murray Darling Basin": ["Botaurus poiciloptilus", "Leipoa ocellata", "Macrotis lagotis", "Myrmecobius fasciatus", "Neophema chrysogaster", "Numenius madagascariensis", "Olearia pannosa subsp. pannosa", "Pedionomus torquatus", "Prasophyllum murfetii", "Stipiturus mallee", "Thelymitra cyanapicata", "Thinornis rubricollis rubricollis"],
    "South East": ["Botaurus poiciloptilus", "Calyptorhynchus banksii graptogyne", "Lathamus discolor", "Leipoa ocellata", "Neophema chrysogaster", "Numenius madagascariensis", "Olearia pannosa subsp. pannosa", "Pedionomus torquatus", "Stipiturus mallee", "Thinornis rubricollis rubricollis"],
    "Cape York": ["Casuarius casuarius johnsonii", "Myrmecodia beccarii", "Numenius madagascariensis", "Psephotus chrysopterygius"],
    "Co-operative Management Area": ["Casuarius casuarius johnsonii", "Numenius madagascariensis", "Psephotus chrysopterygius"],
    "Burdekin": ["Casuarius casuarius johnsonii", "Macrotis lagotis", "Myrmecodia beccarii", "Numenius madagascariensis", "Petaurus gracilis"],
    "Torres Strait": ["Casuarius casuarius johnsonii", "Numenius madagascariensis"],
    "Alinytjara Wilurara": ["Leipoa ocellata", "Petrogale lateralis MacDonnell Ranges race"],
    "Northern and Yorke": ["Leipoa ocellata", "Numenius madagascariensis", "Olearia pannosa subsp. pannosa", "Pedionomus torquatus", "Thinornis rubricollis rubricollis"],
    "South Australian Arid Lands": ["Leipoa ocellata", "Macrotis lagotis", "Myrmecobius fasciatus", "Olearia pannosa subsp. pannosa", "Pedionomus torquatus", "Pezoporus occidentalis"],
    "North Coast - Lord Howe Island": ["Lepidorrhachis mooreana", "Numenius madagascariensis"],
    "Desert Channels": ["Macrotis lagotis", "Pedionomus torquatus", "Pezoporus occidentalis"],
    "Southern Gulf": ["Macrotis lagotis", "Numenius madagascariensis", "Pezoporus occidentalis"],
    "Mackay Whitsunday": ["Numenius madagascariensis"]
};


var now = ISODate();
var programStart = ISODate('2018-06-30T14:00:00Z');
var programEnd = ISODate('2023-06-30T13:59:59Z');


var nlp = {
    name:"National Landcare Programme",
    programId:UUID.generate(),
    dateCreated:now,
    lastUpdated:now,
    status:'active'
};

var nlpProgram = db.program.find({name:nlp.name});
if (nlpProgram.hasNext()) {
    var nlp2 = nlpProgram.next();
    nlp._id = nlp2._id;
    nlp.programId = nlp2.programId;
}
db.program.save(nlp);

var parentId = db.program.find({programId:nlp.programId}).next()._id;

var program = {
    name:"Regional Landcare Program",
    programId:UUID.generate(),
    dateCreated:now,
    lastUpdated:now,
    startDate:programStart,
    parent:parentId,
    endDate:programEnd,
    status:'active',
    visibility:'private',
    config:{
        "meriPlanTemplate":"rlpMeriPlan",
        "projectTemplate":"rlp",
        "activityPeriodDescriptor":"Outputs report #",
        "requiresActivityLocking":true,
        "navigationMode":"returnToProject"

    }
};

var rlpProgram = db.program.find({name:program.name});
if (rlpProgram.hasNext()) {
    var rlp2 = rlpProgram.next();
    program._id = rlp2._id;
    program.programId = rlp2.programId;
}
db.program.save(program);
print("Saving program: ");
printjson(program);

parentId = db.program.find({programId:program.programId}).next()._id;

for (var i=0; i<mus.length; i++) {
    var mu = {
        name:mus[i],
        programId:UUID.generate(),
        status:'active',
        dateCreated:now,
        lastUpdated:now,
        startDate:programStart,
        endDate:programEnd,
        parent:parentId,
        config: {
            "projectReports": [
                {
                    "category":"Outputs Reporting",
                    "reportType": "Activity",
                    "activityType": "RLP Output Report",
                    "reportNameFormat": "Outputs Report %d",
                    "reportDescriptionFormat": "Outputs Report %d for %4$s"
                },
                {
                    "category":"Annual Progress Reporting",
                    "reportsAlignedToCalendar":false,
                    "reportingPeriodInMonths": 12,
                    "reportType": "Administrative",
                    "activityType": "RLP Annual Report",
                    "reportNameFormat": "Annual Progress Report %2$tY - %3$tY",
                    "reportDescriptionFormat": "Annual Progress Report %2$tY - %3$tY for %4$s"
                },
                {
                    "category":"Outcomes Report 1",
                    "reportsAlignedToCalendar": false,
                    "reportingPeriodInMonths": 36,
                    "reportType": "Single",
                    "activityType": "RLP Short term project outcomes",
                    "reportNameFormat": "Outcomes Report 1",
                    "reportDescriptionFormat": "Outcomes Report 1 for %4$s",
                    "multiple":false
                },
                {
                    "category":"Outcomes Report 2",
                    "reportsAlignedToCalendar": false,
                    "reportingPeriodInMonths": 0,
                    "reportType": "Single",
                    "activityType": "RLP Medium term project outcomes",
                    "reportNameFormat": "Outcomes Report 2",
                    "reportDescriptionFormat": "Outcomes Report 2 for %4$s",
                    "multiple":false,
                    "minimumPeriodInMonths":37
                }
            ],
            "programReports":[
                {
                    "reportDescriptionFormat": "Core services report %d for %4$s",
                    "category": "Core Services Reporting",
                    "reportType": "Administrative",
                    "reportNameFormat": "Core services report %d",
                    "activityType":"RLP Core Services report"
                },
                {
                    "reportDescriptionFormat": "Core services annual report %d for %4$s",
                    "reportsAlignedToCalendar":false,
                    "reportingPeriodInMonths": 12,
                    "category": "Core Services Annual Reporting",
                    "reportType": "Administrative",
                    "reportNameFormat": "Core services annual report %d",
                    "activityType":"RLP Core Services annual report"
                }
            ]
        }
    };

    mu.priorities = [];
    var priorities = ramsarByMu[mus[i]] || [];
    for (var j=0; j<priorities.length;j++) {
        mu.priorities.push({category:'Ramsar', priority:priorities[j]})
    };
    priorities = tssSpeciesByMu[mus[i]] || [];
    for (var j=0; j<priorities.length;j++) {
        mu.priorities.push({category:'Threatened Species', priority:priorities[j]})
    };
    priorities = teCommunitesByMu[mus[i]] || [];
    for (var j=0; j<priorities.length;j++) {
        mu.priorities.push({category:'Threatened Ecological Communities', priority:priorities[j]})
    };
    priorities = worldHeritageSitesByMu[mus[i]] || [];
    for (var j=0; j<priorities.length;j++) {
        mu.priorities.push({category:'World Heritage Sites', priority:priorities[j]})
    };
    priorities = soilPrioritiesByMU[mus[i]] || [];
    for (var j=0; j<priorities.length;j++) {
        mu.priorities.push({category:'Soil Quality', priority:priorities[j]})
    };

    var landManagementPriorities = ['Soil acidification', 'Soil erosion', 'Hillslope erosion', 'Wind erosion', 'Native vegetation and biodiversity on-farm'];
    for (var j=0; j<landManagementPriorities.length; j++) {
        mu.priorities.push({category:'Land Management', priority:landManagementPriorities[j]});
    }
    mu.priorities.push({category:'Sustainable Agriculture', priority:'Climate change adaptation'});
    mu.priorities.push({category:'Sustainable Agriculture', priority:'Market traceability'});


    mu.outcomes = [
        {outcome:"By 2023, there is restoration of, and reduction in threats to, the ecological character of Ramsar sites, through the implementation of priority actions", priorities:[{category:"Ramsar"}], category:"environment"},
        {outcome:"By 2023, the trajectory of species targeted under the Threatened Species Strategy, and other EPBC Act priority species, is stabilised or improved.", priorities:[{category:"Threatened Species"}], category:"environment"},
        {outcome:"By 2023, invasive species management has reduced threats to the natural heritage Outstanding Universal Value of World Heritage properties through the implementation of priority actions.", priorities:[{category:"World Heritage Sites"}], category:"environment"},
        {outcome:"By 2023, the implementation of priority actions is leading to an improvement in the condition of EPBC Act listed Threatened Ecological Communities.", priorities:[{category:"Threatened Ecological Communities"}], category:"environment"},
        {outcome:"By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation.", priorities:[{category:"Land Management"}], category:"agriculture"},
        {outcome:"By 2023, there is an increase in the capacity of agriculture systems to adapt to significant changes in climate and market demands for information on provenance and sustainable production.", priorities:[{category:"Sustainable Agriculture"}], category:"agriculture"}
    ];


    var existingMu = db.program.find({name:mu.name});
    if (existingMu.hasNext()) {
        var mu2 = existingMu.next();
        mu.description = mu2.description;
        mu._id = mu2._id;
        mu.programId = mu2.programId;

    }

    db.program.save(mu);

}