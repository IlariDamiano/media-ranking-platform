import { BrowserRouter, Route, Routes } from "react-router";

import SiteLayout from "../layouts/SiteLayout";
import DiscoverPage from "../pages/DiscoverPage";
import PlaceholderPage from "../pages/PlaceholderPage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<SiteLayout />}>
          <Route index element={<DiscoverPage />} />

          <Route
            path="library"
            element={
              <PlaceholderPage
                title="YOUR LIBRARY."
                description="Your ratings, progress and saved stories will live here."
              />
            }
          />

          <Route
            path="lists"
            element={
              <PlaceholderPage
                title="YOUR COLLECTIONS."
                description="Create, organize and share media rankings with friends."
              />
            }
          />

          <Route
            path="*"
            element={
              <PlaceholderPage
                title="404 / LOST IN THE ARCHIVE."
                description="This page doesn't exist."
              />
            }
          />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
